package com.vivance.hotel.service;

import com.vivance.hotel.config.AggregatorProperties;
import com.vivance.hotel.dto.response.TboHotelStaticDetailsSyncResult;
import com.vivance.hotel.infrastructure.aggregator.tbo.TboStaticDataService;
import com.vivance.hotel.infrastructure.aggregator.tbo.dto.TboStaticHotelDetailsRequest;
import com.vivance.hotel.infrastructure.aggregator.tbo.dto.TboStaticHotelDetailsResponse;
import com.vivance.hotel.repository.TboHotelStaticRepository;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Qualifier;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageRequest;
import org.springframework.stereotype.Service;

import java.util.ArrayList;
import java.util.List;
import java.util.Locale;
import java.util.Objects;
import java.util.concurrent.CompletableFuture;
import java.util.concurrent.CompletionException;
import java.util.concurrent.ExecutorService;

/**
 * Loads {@code hotel_code} values from {@code tbo_hotels_static} that are not yet stored in
 * {@code tbo_hotel_static_details} with a non-null {@code fetched_at} (resume-safe), optionally filtered
 * by {@code city_name} for non-production environments, calls TBO static Hoteldetails in batches, and upserts.
 */
@Slf4j
@Service
@RequiredArgsConstructor
public class TboHotelStaticDetailsSyncService {

    private final TboHotelStaticRepository tboHotelStaticRepository;
    private final TboHotelStaticDetailsPersistenceService persistenceService;
    private final TboStaticDataService tboStaticDataService;
    private final AggregatorProperties aggregatorProperties;

    @Qualifier("tboStaticHotelDetailsExecutor")
    private final ExecutorService tboStaticHotelDetailsExecutor;

    public TboHotelStaticDetailsSyncResult syncAllHotelDetailsFromStaticTable() {
        AggregatorProperties.TboConfig tbo = aggregatorProperties.getTbo();
        int batchSize = Math.max(1, tbo.getStaticHotelDetailsBatchSize());
        int concurrency = Math.max(1, tbo.getStaticHotelDetailsMaxConcurrentRequests());
        int pageSize = batchSize * concurrency;

        List<String> cityFilter = normalizedCityFilter(tbo.getStaticHotelDetailsRestrictToCityNames());
        long totalStaticRows = tboHotelStaticRepository.count();
        long pendingAtStart = cityFilter.isEmpty()
                ? tboHotelStaticRepository.countHotelCodesPendingDetailSync()
                : tboHotelStaticRepository.countHotelCodesPendingDetailSyncForCities(cityFilter);

        if (!cityFilter.isEmpty()) {
            log.info("[TBO-STATIC] Hoteldetails sync city filter active ({} names, case-insensitive): {}",
                    cityFilter.size(), cityFilter);
        } else {
            log.info("[TBO-STATIC] Hoteldetails sync: all cities (no city filter; production-style)");
        }
        log.info("[TBO-STATIC] Resume baseline — pending hotel codes: {} (total static rows: {})",
                pendingAtStart, totalStaticRows);

        long startMs = System.currentTimeMillis();

        int apiBatchesAttempted = 0;
        int batchesFailed = 0;
        int hotelsSavedOrUpdated = 0;
        List<String> errorSamples = new ArrayList<>();

        for (int pageNum = 0; ; pageNum++) {
            Page<String> page = fetchPendingPage(pageNum, pageSize, cityFilter);
            if (page.isEmpty()) {
                break;
            }

            List<List<String>> batches = partition(page.getContent(), batchSize);
            List<CompletableFuture<BatchOutcome>> futures = new ArrayList<>();
            for (List<String> batch : batches) {
                List<String> b = List.copyOf(batch);
                futures.add(CompletableFuture.supplyAsync(() -> fetchAndUpsertBatch(b), tboStaticHotelDetailsExecutor));
            }

            apiBatchesAttempted += futures.size();

            for (CompletableFuture<BatchOutcome> future : futures) {
                try {
                    BatchOutcome outcome = future.join();
                    if (outcome.errorMessage != null) {
                        batchesFailed++;
                        addErrorSample(errorSamples, outcome.errorMessage);
                    } else {
                        hotelsSavedOrUpdated += outcome.savedCount;
                    }
                } catch (CompletionException e) {
                    batchesFailed++;
                    Throwable cause = e.getCause() != null ? e.getCause() : e;
                    String msg = cause.getMessage() != null ? cause.getMessage() : cause.toString();
                    addErrorSample(errorSamples, msg);
                    log.warn("[TBO-STATIC] Hoteldetails batch completion failed: {}", msg);
                }
            }
        }

        long durationMs = System.currentTimeMillis() - startMs;
        log.info("[TBO-STATIC] Hoteldetails sync done: pendingAtStart={} batches={} savedOrUpdated={} failedBatches={} tookMs={}",
                pendingAtStart, apiBatchesAttempted, hotelsSavedOrUpdated, batchesFailed, durationMs);

        return TboHotelStaticDetailsSyncResult.builder()
                .totalRowsInTboHotelsStatic(totalStaticRows)
                .pendingHotelCodesAtStart(pendingAtStart)
                .cityFilterNormalized(cityFilter.isEmpty() ? List.of() : List.copyOf(cityFilter))
                .apiBatchesAttempted(apiBatchesAttempted)
                .hotelsSavedOrUpdated(hotelsSavedOrUpdated)
                .batchesFailed(batchesFailed)
                .durationMs(durationMs)
                .errorSamples(errorSamples.isEmpty() ? List.of() : List.copyOf(errorSamples))
                .build();
    }

    private Page<String> fetchPendingPage(int pageNum, int pageSize, List<String> cityFilterUpper) {
        PageRequest pr = PageRequest.of(pageNum, pageSize);
        if (cityFilterUpper.isEmpty()) {
            return tboHotelStaticRepository.findHotelCodesPendingDetailSync(pr);
        }
        return tboHotelStaticRepository.findHotelCodesPendingDetailSyncForCities(cityFilterUpper, pr);
    }

    private static List<String> normalizedCityFilter(List<String> raw) {
        if (raw == null || raw.isEmpty()) {
            return List.of();
        }
        return raw.stream()
                .filter(Objects::nonNull)
                .map(s -> s.trim().toUpperCase(Locale.ROOT))
                .filter(s -> !s.isEmpty())
                .distinct()
                .toList();
    }

    private BatchOutcome fetchAndUpsertBatch(List<String> batch) {
        if (batch.isEmpty()) {
            return new BatchOutcome(0, null);
        }
        try {
            TboStaticHotelDetailsRequest req = TboStaticHotelDetailsRequest.builder()
                    .hotelcodes(String.join(",", batch))
                    .language("EN")
                    .isRoomDetailRequired(true)
                    .build();
            TboStaticHotelDetailsResponse resp = tboStaticDataService.fetchHotelDetails(req);
            int saved = persistenceService.upsertFromResponse(resp);
            return new BatchOutcome(saved, null);
        } catch (Exception e) {
            log.warn("[TBO-STATIC] Hoteldetails batch failed (size={}): {}", batch.size(), e.getMessage());
            return new BatchOutcome(0, "batch n=" + batch.size() + ": " + e.getMessage());
        }
    }

    private static void addErrorSample(List<String> errorSamples, String message) {
        if (errorSamples.size() >= 10 || message == null) {
            return;
        }
        errorSamples.add(message);
    }

    private static <T> List<List<T>> partition(List<T> list, int size) {
        List<List<T>> parts = new ArrayList<>();
        for (int i = 0; i < list.size(); i += size) {
            parts.add(list.subList(i, Math.min(i + size, list.size())));
        }
        return parts;
    }

    private record BatchOutcome(int savedCount, String errorMessage) {
    }
}
