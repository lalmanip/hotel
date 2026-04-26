package com.vivance.hotel.service;

import com.vivance.hotel.config.HotelSearchProperties;
import com.vivance.hotel.dto.request.HotelSearchRequest;
import com.vivance.hotel.dto.request.HotelSearchV1Request;
import com.vivance.hotel.dto.response.HotelSearchV1Response;
import com.vivance.hotel.exception.AggregatorException;
import com.vivance.hotel.infrastructure.aggregator.tbo.TboAggregatorService;
import com.vivance.hotel.infrastructure.aggregator.tbo.dto.TboAffiliateSearchRequest;
import com.vivance.hotel.infrastructure.aggregator.tbo.dto.TboAffiliateSearchResponse;
import com.vivance.hotel.repository.TboHotelStaticRepository;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Qualifier;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageRequest;
import org.springframework.stereotype.Service;

import java.math.BigDecimal;
import java.util.ArrayList;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.concurrent.CompletableFuture;
import java.util.concurrent.Executor;
import java.util.stream.Collectors;

@Slf4j
@Service
@RequiredArgsConstructor
public class HotelSearchV1Service {

    private final TboHotelStaticRepository tboHotelStaticRepository;
    private final TboAggregatorService tboAggregatorService;
    private final HotelSearchProperties props;
    private final HotelSearchV1Cache cache;

    @Qualifier("hotelSearchChunkExecutor")
    private final Executor hotelSearchChunkExecutor;

    /**
     * Returns a merged raw TBO affiliate search response (HotelResult is concatenated + deduped by HotelCode).
     * This preserves fields like BookingCode, CancelPolicies, etc.
     */
    public TboAffiliateSearchResponse searchRaw(HotelSearchV1Request request) {
        validateRequest(request);

        ResolvedCodes resolved = resolveHotelCodes(request);
        if (resolved.hotelCodes.isEmpty()) {
            TboAffiliateSearchResponse empty = new TboAffiliateSearchResponse();
            var st = new com.vivance.hotel.infrastructure.aggregator.tbo.dto.TboAffiliateStatus();
            st.setCode(200);
            st.setDescription("Successful (no hotel codes found)");
            empty.setStatus(st);
            empty.setHotelResult(List.of());
            return empty;
        }

        HotelSearchRequest tboBase = toTboBaseRequest(request);
        String tokenId = tboAggregatorService.resolveAffiliateSearchToken(tboBase);

        int chunkSize = Math.max(1, props.getTboHotelCodesChunkSize());
        List<List<String>> chunks = partition(resolved.hotelCodes, chunkSize);

        List<CompletableFuture<ChunkResult>> futures = new ArrayList<>(chunks.size());
        for (int i = 0; i < chunks.size(); i++) {
            final int chunkIndex = i;
            final List<String> chunkCodes = chunks.get(i);
            futures.add(CompletableFuture.supplyAsync(
                    () -> callTboChunk(tboBase, tokenId, chunkCodes, chunkIndex),
                    hotelSearchChunkExecutor
            ));
        }

        CompletableFuture.allOf(futures.toArray(CompletableFuture[]::new)).join();

        Map<String, TboAffiliateSearchResponse.HotelResult> deduped = new LinkedHashMap<>();
        int failures = 0;
        for (CompletableFuture<ChunkResult> f : futures) {
            ChunkResult r = f.getNow(null);
            if (r == null || r.response.isEmpty()) {
                failures++;
                continue;
            }
            TboAffiliateSearchResponse resp = r.response.get();
            if (resp.getHotelResult() == null) continue;
            for (TboAffiliateSearchResponse.HotelResult hr : resp.getHotelResult()) {
                if (hr == null || hr.getHotelCode() == null || hr.getHotelCode().isBlank()) continue;
                deduped.putIfAbsent(hr.getHotelCode(), hr);
            }
        }

        TboAffiliateSearchResponse merged = new TboAffiliateSearchResponse();
        var st = new com.vivance.hotel.infrastructure.aggregator.tbo.dto.TboAffiliateStatus();
        st.setCode(200);
        st.setDescription(failures > 0
                ? "Successful (partial): " + failures + "/" + chunks.size() + " chunk(s) failed"
                : "Successful");
        merged.setStatus(st);
        merged.setHotelResult(new ArrayList<>(deduped.values()));

        log.info("[HOTEL-SEARCH] Raw merge summary: codes={}, chunks={}, uniqueHotels={}, failedChunks={}",
                resolved.hotelCodes.size(), chunks.size(), deduped.size(), failures);

        return merged;
    }

    public HotelSearchV1Response search(HotelSearchV1Request request) {
        validateRequest(request);

        String cacheKey = cacheKey(request);
        HotelSearchV1Response cached = cache.getIfPresent(cacheKey);
        if (cached != null) {
            return cached;
        }

        ResolvedCodes resolved = resolveHotelCodes(request);
        if (resolved.hotelCodes.isEmpty()) {
            return HotelSearchV1Response.builder()
                    .hotels(List.of())
                    .pagination(HotelSearchV1Response.Pagination.builder()
                            .total(resolved.total)
                            .page(resolved.page)
                            .size(resolved.size)
                            .build())
                    .build();
        }

        HotelSearchRequest tboBase = toTboBaseRequest(request);
        String tokenId = tboAggregatorService.resolveAffiliateSearchToken(tboBase);

        int chunkSize = Math.max(1, props.getTboHotelCodesChunkSize());
        List<List<String>> chunks = partition(resolved.hotelCodes, chunkSize);

        List<CompletableFuture<ChunkResult>> futures = new ArrayList<>(chunks.size());
        for (int i = 0; i < chunks.size(); i++) {
            final int chunkIndex = i;
            final List<String> chunkCodes = chunks.get(i);
            futures.add(CompletableFuture.supplyAsync(
                    () -> callTboChunk(tboBase, tokenId, chunkCodes, chunkIndex),
                    hotelSearchChunkExecutor
            ));
        }

        CompletableFuture.allOf(futures.toArray(CompletableFuture[]::new)).join();

        List<TboAffiliateSearchResponse.HotelResult> merged = new ArrayList<>();
        int failures = 0;
        int nullHotelResultCount = 0;
        for (CompletableFuture<ChunkResult> f : futures) {
            ChunkResult r = f.getNow(null);
            if (r == null || r.response.isEmpty()) {
                failures++;
                continue;
            }
            TboAffiliateSearchResponse resp = r.response.get();
            if (resp.getHotelResult() == null) {
                nullHotelResultCount++;
                continue;
            }
            if (!resp.getHotelResult().isEmpty()) {
                merged.addAll(resp.getHotelResult());
            }
        }

        Map<String, HotelSearchV1Response.HotelSearchResultDto> deduped = new LinkedHashMap<>();
        for (TboAffiliateSearchResponse.HotelResult hr : merged) {
            if (hr == null || hr.getHotelCode() == null || hr.getHotelCode().isBlank()) continue;
            deduped.putIfAbsent(hr.getHotelCode(), mapHotel(hr));
        }

        List<HotelSearchV1Response.HotelSearchResultDto> hotels = new ArrayList<>(deduped.values());
        hotels = applyFilters(hotels, request.getFilters());

        log.info("[HOTEL-SEARCH] Aggregation summary: codes={}, chunks={}, mergedHotelResults={}, uniqueHotels={}, nullHotelResultChunks={}, failedChunks={}",
                resolved.hotelCodes.size(), chunks.size(), merged.size(), hotels.size(), nullHotelResultCount, failures);

        if (failures > 0) {
            log.warn("[HOTEL-SEARCH] Partial failures: {}/{} TBO chunk(s) failed (destinationId={}, hotelCode={})",
                    failures, chunks.size(), request.getDestinationId(), request.getHotelCode());
        }

        HotelSearchV1Response response = HotelSearchV1Response.builder()
                .hotels(hotels)
                .pagination(HotelSearchV1Response.Pagination.builder()
                        .total(resolved.total)
                        .page(resolved.page)
                        .size(resolved.size)
                        .build())
                .build();

        // Avoid caching "mysteriously empty" responses when TBO calls succeeded but deserialization yielded no hotels.
        // This prevents 5-minute stale empties while we diagnose supplier/DTO mismatches.
        if (!hotels.isEmpty() || failures == chunks.size()) {
            cache.put(cacheKey, response);
        }
        return response;
    }

    private static void validateRequest(HotelSearchV1Request r) {
        if (r.getCheckIn() != null && r.getCheckOut() != null && !r.getCheckOut().isAfter(r.getCheckIn())) {
            throw new IllegalArgumentException("checkOut must be after checkIn");
        }
        if ((r.getHotelCode() == null || r.getHotelCode().isBlank()) && (r.getDestinationId() == null || r.getDestinationId() <= 0)) {
            throw new IllegalArgumentException("Provide either hotelCode or destinationId");
        }
    }

    private ResolvedCodes resolveHotelCodes(HotelSearchV1Request request) {
        int page = Math.max(1, request.getPagination().getPage());
        int size = Math.max(1, request.getPagination().getSize());

        if (request.hasHotelCode()) {
            return new ResolvedCodes(List.of(request.getHotelCode().trim()), 1, page, size);
        }

        String cityCode = String.valueOf(request.getDestinationId());
        long total = tboHotelStaticRepository.countByCityCode(cityCode);
        Page<String> codesPage = tboHotelStaticRepository.findHotelCodesByCityCode(
                cityCode,
                PageRequest.of(page - 1, size)
        );
        List<String> codes = codesPage.getContent().stream()
                .filter(c -> c != null && !c.isBlank())
                .map(String::trim)
                .toList();

        return new ResolvedCodes(codes, total, page, size);
    }

    private ChunkResult callTboChunk(HotelSearchRequest base, String tokenId, List<String> codes, int chunkIndex) {
        if (codes.isEmpty()) {
            return new ChunkResult(chunkIndex, Optional.empty(), 0);
        }

        String csv = String.join(",", codes);
        TboAffiliateSearchRequest tboReq = tboAggregatorService.buildAffiliateSearchRequest(base, csv, tokenId, null);

        long start = System.nanoTime();
        Optional<TboAffiliateSearchResponse> resp = tboAggregatorService.postAffiliateSearchRawLenient(
                "TboAffiliateSearchChunk[" + chunkIndex + "|n=" + codes.size() + "]",
                tboReq
        );
        long tookMs = (System.nanoTime() - start) / 1_000_000L;
        log.info("[HOTEL-SEARCH] TBO chunk {} (n={}) took {}ms success={}",
                chunkIndex, codes.size(), tookMs, resp.isPresent());

        return new ChunkResult(chunkIndex, resp, tookMs);
    }

    private static List<HotelSearchV1Response.HotelSearchResultDto> applyFilters(
            List<HotelSearchV1Response.HotelSearchResultDto> hotels,
            HotelSearchV1Request.Filters filters) {

        if (filters == null) return hotels;

        Double min = filters.getPriceMin();
        Double max = filters.getPriceMax();

        // Treat 0 / negative values as "filter not applied" (frontend often sends 0 as default).
        if (min != null && min > 0) {
            hotels = hotels.stream()
                    .filter(h -> h.getPrice() == null || h.getPrice() >= min)
                    .toList();
        }
        if (max != null && max > 0) {
            hotels = hotels.stream()
                    .filter(h -> h.getPrice() == null || h.getPrice() <= max)
                    .toList();
        }
        if (filters.getStars() != null && !filters.getStars().isEmpty()) {
            var stars = filters.getStars().stream().filter(s -> s != null).collect(Collectors.toSet());
            hotels = hotels.stream()
                    // If rating is absent from supplier response, keep it (don't hard-drop everything).
                    .filter(h -> h.getRating() == null || stars.contains((int) Math.floor(h.getRating())))
                    .toList();
        }
        return hotels;
    }

    private static HotelSearchV1Response.HotelSearchResultDto mapHotel(TboAffiliateSearchResponse.HotelResult hr) {
        BigDecimal minTotal = null;
        List<String> amenities = new ArrayList<>();
        if (hr.getRooms() != null) {
            for (TboAffiliateSearchResponse.Room r : hr.getRooms()) {
                if (r == null) continue;
                BigDecimal total = r.getTotalFare();
                if (total != null && r.getTotalTax() != null) {
                    total = total.add(r.getTotalTax());
                }
                if (total != null) {
                    minTotal = (minTotal == null) ? total : minTotal.min(total);
                }
                if (r.getAmenities() != null) {
                    amenities.addAll(r.getAmenities());
                }
            }
        }

        Double price = minTotal != null ? minTotal.doubleValue() : null;
        List<String> distinctAmenities = amenities.stream().filter(a -> a != null && !a.isBlank()).distinct().toList();

        return HotelSearchV1Response.HotelSearchResultDto.builder()
                .hotelCode(hr.getHotelCode())
                .name("Hotel " + hr.getHotelCode())
                .price(price)
                .rating(null)
                .location(null)
                .amenities(distinctAmenities)
                .build();
    }

    private static List<List<String>> partition(List<String> codes, int chunkSize) {
        if (codes.isEmpty()) return List.of();
        List<List<String>> parts = new ArrayList<>((codes.size() + chunkSize - 1) / chunkSize);
        for (int i = 0; i < codes.size(); i += chunkSize) {
            parts.add(codes.subList(i, Math.min(i + chunkSize, codes.size())));
        }
        return parts;
    }

    private static HotelSearchRequest toTboBaseRequest(HotelSearchV1Request r) {
        HotelSearchRequest base = new HotelSearchRequest();
        base.setCheckIn(r.getCheckIn());
        base.setCheckOut(r.getCheckOut());
        base.setGuestNationality("IN");
        base.setIsDetailedResponse(true);
        base.setResponseTime(23.0);
        base.setPaxRooms(r.getRooms().stream().map(room -> {
            HotelSearchRequest.PaxRoom pr = new HotelSearchRequest.PaxRoom();
            pr.setAdults(room.getAdults());
            pr.setChildren(room.getChildren() != null ? room.getChildren().size() : 0);
            pr.setChildrenAges(room.getChildren());
            return pr;
        }).toList());
        return base;
    }

    private static String cacheKey(HotelSearchV1Request r) {
        String roomsFp = r.getRooms().stream()
                .map(x -> x.getAdults() + ":" + (x.getChildren() != null ? x.getChildren().stream().map(String::valueOf).collect(Collectors.joining(",")) : ""))
                .collect(Collectors.joining("|"));
        String filtersFp = "";
        if (r.getFilters() != null) {
            filtersFp = (r.getFilters().getPriceMin() != null ? r.getFilters().getPriceMin() : "")
                    + "-" + (r.getFilters().getPriceMax() != null ? r.getFilters().getPriceMax() : "")
                    + "-" + (r.getFilters().getStars() != null ? r.getFilters().getStars().toString() : "");
        }
        return (r.getDestinationId() != null ? "d" + r.getDestinationId() : "")
                + "|h" + (r.getHotelCode() != null ? r.getHotelCode().trim() : "")
                + "|" + r.getCheckIn()
                + "|" + r.getCheckOut()
                + "|" + roomsFp
                + "|" + filtersFp
                + "|p" + r.getPagination().getPage()
                + "|s" + r.getPagination().getSize();
    }

    private record ResolvedCodes(List<String> hotelCodes, long total, int page, int size) {}

    private record ChunkResult(int index, Optional<TboAffiliateSearchResponse> response, long tookMs) {}
}

