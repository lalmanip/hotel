package com.vivance.hotel.service;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.vivance.hotel.domain.entity.ApiSessionItinerary;
import com.vivance.hotel.repository.ApiSessionItineraryRepository;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Slf4j
@Service
@RequiredArgsConstructor
public class ApiSessionItineraryService {

    private final ApiSessionItineraryRepository apiSessionItineraryRepository;
    private final ObjectMapper objectMapper;

    @Transactional
    public void recordPreBook(String traceId, String bookingCode, Object preBookResponse) {
        if (traceId == null || traceId.isBlank()) {
            log.debug("[ITINERARY] Skip api_session_itinerary write: missing traceId");
            return;
        }
        if (bookingCode == null || bookingCode.isBlank()) {
            log.debug("[ITINERARY] Skip api_session_itinerary write: missing bookingCode");
            return;
        }

        String json;
        try {
            json = objectMapper.writeValueAsString(preBookResponse);
        } catch (Exception e) {
            log.warn("[ITINERARY] Failed serializing PreBook response for traceId={}: {}", traceId, e.getMessage());
            json = null;
        }

        // Create a new itinerary row per step for auditability (rather than overwriting).
        ApiSessionItinerary row = new ApiSessionItinerary();
        row.setTraceId(traceId);
        row.setSelectedResultToken(bookingCode);
        row.setSelectedJourney(json);
        row.setProgressStep("PreBook");
        apiSessionItineraryRepository.save(row);
    }
}

