package com.vivance.hotel.service;

import com.vivance.hotel.dto.request.HotelSearchRequest;
import com.vivance.hotel.exception.AggregatorException;
import com.vivance.hotel.infrastructure.aggregator.tbo.TboAggregatorService;
import com.vivance.hotel.infrastructure.aggregator.tbo.dto.TboAffiliateSearchRequest;
import com.vivance.hotel.infrastructure.aggregator.tbo.dto.TboAffiliateSearchResponse;
import com.vivance.hotel.infrastructure.aggregator.tbo.dto.TboAffiliateStatus;
import com.vivance.hotel.repository.TboHotelStaticRepository;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Qualifier;
import org.springframework.stereotype.Service;

import java.util.AbstractMap.SimpleEntry;
import java.util.ArrayList;
import java.util.List;
import java.util.Optional;
import java.util.Set;
import java.util.concurrent.CompletableFuture;
import java.util.concurrent.ConcurrentSkipListSet;
import java.util.concurrent.Executor;
import java.util.concurrent.TimeUnit;
import java.util.stream.Collectors;
import java.util.stream.IntStream;

/**
 * Static-city hotel search: resolves hotel codes from {@code tbo_hotels_static}, then calls TBO
 * affiliate Search <strong>once per hotel code</strong> (a single code in {@code HotelCodes} per
 * request — comma-separated lists are not supported by TBO). Up to 50 such calls run in parallel;
 * the HTTP response returns after a {@code ResponseTime} budget (default ~7s), then remaining
 * hotel-level calls continue in the background and the fully merged result is cached.
 */
@Service
@Slf4j
public class TboHotelChunkSearchOrchestrator {

    private static final int MAX_PARALLEL_CHUNK_CALLS = 50;
    private static final double DEFAULT_RESPONSE_TIME_SEC = 7.0;

    private final TboHotelStaticRepository staticRepository;
    private final TboAggregatorService tboAggregatorService;
    private final HotelStaticChunkSearchCache chunkSearchCache;
    private final Executor hotelSearchChunkExecutor;

    public TboHotelChunkSearchOrchestrator(
            TboHotelStaticRepository staticRepository,
            TboAggregatorService tboAggregatorService,
            HotelStaticChunkSearchCache chunkSearchCache,
            @Qualifier("hotelSearchChunkExecutor") Executor hotelSearchChunkExecutor) {
        this.staticRepository = staticRepository;
        this.tboAggregatorService = tboAggregatorService;
        this.chunkSearchCache = chunkSearchCache;
        this.hotelSearchChunkExecutor = hotelSearchChunkExecutor;
    }

    public boolean supportsStaticCityChunkSearch(HotelSearchRequest request) {
        return request.getCityCode() != null && !request.getCityCode().isBlank()
                && request.getCountryCode() != null && !request.getCountryCode().isBlank();
    }

    public TboAffiliateSearchResponse searchByStaticCity(HotelSearchRequest request) {
        String cityCode = request.getCityCode().trim();
        String countryCode = request.getCountryCode().trim();

        String cacheKey = buildCacheKey(request, cityCode, countryCode);
        TboAffiliateSearchResponse cached = chunkSearchCache.getIfPresent(cacheKey);
        if (cached != null) {
            log.info("[TBO] Chunk search cache hit for cityCode={}, country={}", cityCode, countryCode);
            return cached;
        }

        List<String> allCodes = staticRepository.findDistinctHotelCodesByCityCodeAndCountryCode(cityCode, countryCode);
        if (allCodes.isEmpty()) {
            throw new AggregatorException(
                    "No hotel codes in tbo_hotels_static for Citycode=" + cityCode + " and CountryCode=" + countryCode);
        }

        List<List<String>> chunks = partitionSingleHotelJobs(allCodes);
        double tboResponseTime = resolveTboResponseTimeSeconds(request);
        double budgetSec = tboResponseTime;

        String token = tboAggregatorService.resolveAffiliateSearchToken(request);

        Set<Integer> pending = new ConcurrentSkipListSet<>(
                IntStream.range(0, chunks.size()).boxed().collect(Collectors.toSet()));

        int firstWaveSize = Math.min(MAX_PARALLEL_CHUNK_CALLS, chunks.size());
        List<CompletableFuture<SimpleEntry<Integer, Optional<TboAffiliateSearchResponse>>>> firstWave = new ArrayList<>();
        for (int i = 0; i < firstWaveSize; i++) {
            final int chunkIndex = i;
            firstWave.add(CompletableFuture.supplyAsync(
                    () -> new SimpleEntry<>(chunkIndex, executeChunk(request, token, chunks.get(chunkIndex), chunkIndex, tboResponseTime)),
                    hotelSearchChunkExecutor));
        }

        try {
            TimeUnit.MILLISECONDS.sleep((long) (budgetSec * 1000L));
        } catch (InterruptedException e) {
            Thread.currentThread().interrupt();
            throw new AggregatorException("Hotel search interrupted");
        }

        List<TboAffiliateSearchResponse.HotelResult> syncHotels = new ArrayList<>();
        for (CompletableFuture<SimpleEntry<Integer, Optional<TboAffiliateSearchResponse>>> f : firstWave) {
            if (!f.isDone()) {
                continue;
            }
            try {
                SimpleEntry<Integer, Optional<TboAffiliateSearchResponse>> entry = f.getNow(null);
                if (entry == null) {
                    continue;
                }
                entry.getValue().ifPresent(resp -> {
                    pending.remove(entry.getKey());
                    if (resp.getHotelResult() != null) {
                        syncHotels.addAll(resp.getHotelResult());
                    }
                });
            } catch (Exception e) {
                log.debug("[TBO] First-wave chunk future failed: {}", e.getMessage());
            }
        }

        TboAffiliateSearchResponse merged = wrapMergedHotels(
                syncHotels,
                "Merged " + syncHotels.size() + " hotel(s) from first wave; " + pending.size() + " hotel search(es) pending background");

        if (!pending.isEmpty()) {
            List<TboAffiliateSearchResponse.HotelResult> baseline = new ArrayList<>(syncHotels);
            CompletableFuture.runAsync(
                    () -> drainPendingChunks(request, token, chunks, pending, tboResponseTime, baseline, cacheKey),
                    hotelSearchChunkExecutor);
        } else {
            chunkSearchCache.putCompleted(cacheKey, merged);
        }

        return merged;
    }

    private void drainPendingChunks(
            HotelSearchRequest request,
            String token,
            List<List<String>> chunks,
            Set<Integer> pending,
            double tboResponseTime,
            List<TboAffiliateSearchResponse.HotelResult> accumulator,
            String cacheKey) {

        while (!pending.isEmpty()) {
            List<Integer> batch = new ArrayList<>(pending.stream().limit(MAX_PARALLEL_CHUNK_CALLS).toList());
            batch.forEach(pending::remove);

            List<CompletableFuture<SimpleEntry<Integer, Optional<TboAffiliateSearchResponse>>>> wave = new ArrayList<>();
            for (int chunkIndex : batch) {
                wave.add(CompletableFuture.supplyAsync(
                        () -> new SimpleEntry<>(chunkIndex, executeChunk(request, token, chunks.get(chunkIndex), chunkIndex, tboResponseTime)),
                        hotelSearchChunkExecutor));
            }
            CompletableFuture.allOf(wave.toArray(CompletableFuture[]::new)).join();

            for (CompletableFuture<SimpleEntry<Integer, Optional<TboAffiliateSearchResponse>>> f : wave) {
                try {
                    SimpleEntry<Integer, Optional<TboAffiliateSearchResponse>> entry = f.get();
                    Optional<TboAffiliateSearchResponse> opt = entry.getValue();
                    opt.ifPresent(resp -> {
                        if (resp.getHotelResult() != null) {
                            synchronized (accumulator) {
                                accumulator.addAll(resp.getHotelResult());
                            }
                        } else {
                            log.warn("[TBO] Background chunk {} returned no hotels", entry.getKey());
                        }
                    });
                    if (opt.isEmpty()) {
                        log.warn("[TBO] Background chunk {} failed or non-success", entry.getKey());
                    }
                } catch (Exception e) {
                    log.warn("[TBO] Background chunk join failed: {}", e.getMessage());
                }
            }
        }

        TboAffiliateSearchResponse full = wrapMergedHotels(
                new ArrayList<>(accumulator),
                "Merged " + accumulator.size() + " hotel(s) (including background chunks)");
        chunkSearchCache.putCompleted(cacheKey, full);
        log.info("[TBO] Background chunk search finished for cacheKey={} hotels={}", cacheKey, accumulator.size());
    }

    private Optional<TboAffiliateSearchResponse> executeChunk(
            HotelSearchRequest request,
            String token,
            List<String> chunkHotelCodes,
            int chunkIndex,
            double tboResponseTime) {

        if (chunkHotelCodes.size() != 1) {
            throw new IllegalStateException("TBO search expects exactly one hotel code per call, got " + chunkHotelCodes.size());
        }
        String singleHotelCode = chunkHotelCodes.get(0);
        TboAffiliateSearchRequest tboReq = tboAggregatorService.buildAffiliateSearchRequest(
                request, singleHotelCode, token, tboResponseTime);
        return tboAggregatorService.postAffiliateSearchRawLenient(
                "TboAffiliateSearchRawHotel[" + chunkIndex + "|" + singleHotelCode + "]",
                tboReq).filter(TboHotelChunkSearchOrchestrator::isAffiliateSuccess);
    }

    private static boolean isAffiliateSuccess(TboAffiliateSearchResponse r) {
        return r.getStatus() != null
                && r.getStatus().getCode() != null
                && r.getStatus().getCode() == 200;
    }

    private static TboAffiliateSearchResponse wrapMergedHotels(
            List<TboAffiliateSearchResponse.HotelResult> hotels,
            String statusDescription) {

        TboAffiliateSearchResponse merged = new TboAffiliateSearchResponse();
        TboAffiliateStatus st = new TboAffiliateStatus();
        st.setCode(200);
        st.setDescription(statusDescription);
        merged.setStatus(st);
        merged.setHotelResult(hotels);
        return merged;
    }

    private static List<List<String>> partitionSingleHotelJobs(List<String> hotelCodes) {
        List<List<String>> parts = new ArrayList<>(hotelCodes.size());
        for (String code : hotelCodes) {
            parts.add(List.of(code));
        }
        return parts;
    }

    /** TBO recommends a short ResponseTime for parallel city searches; keep within 5–8s. */
    private static double resolveTboResponseTimeSeconds(HotelSearchRequest request) {
        if (request.getResponseTime() != null) {
            return Math.clamp(request.getResponseTime(), 5.0, 8.0);
        }
        return DEFAULT_RESPONSE_TIME_SEC;
    }

    private static String buildCacheKey(HotelSearchRequest r, String cityCode, String countryCode) {
        String pax = paxFingerprint(r);
        return cityCode + "|" + countryCode + "|" + r.getCheckIn() + "|" + r.getCheckOut() + "|" + pax;
    }

    private static String paxFingerprint(HotelSearchRequest r) {
        if (r.getPaxRooms() != null && !r.getPaxRooms().isEmpty()) {
            return r.getPaxRooms().stream()
                    .map(p -> (p.getAdults() != null ? p.getAdults() : 1) + ":" + (p.getChildren() != null ? p.getChildren() : 0))
                    .collect(Collectors.joining(","));
        }
        return "g" + r.effectiveGuests();
    }
}
