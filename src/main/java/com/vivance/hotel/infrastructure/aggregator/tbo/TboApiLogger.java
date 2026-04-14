package com.vivance.hotel.infrastructure.aggregator.tbo;

import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.ObjectMapper;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;

/**
 * Centralised request/response logger for all TBO API calls.
 * Produces structured log lines that can be easily parsed by log aggregation tools.
 */
@Slf4j
@Service
@RequiredArgsConstructor
public class TboApiLogger {

    private final ObjectMapper objectMapper;

    /**
     * Logs an outbound TBO API request.
     *
     * @param operation name of the TBO operation (e.g. "TboAuthenticate", "TboHotelSearch")
     * @param tokenId   the auth token used (null for auth calls)
     * @param payload   the request body object
     */
    public void logRequest(String operation, String tokenId, Object payload) {
        try {
            log.info("[TBO][{}] --> REQUEST | token={} | body={}",
                    operation,
                    tokenId != null ? maskToken(tokenId) : "N/A",
                    objectMapper.writeValueAsString(payload));
        } catch (JsonProcessingException e) {
            log.warn("[TBO][{}] --> REQUEST | token={} | body=<serialization error>",
                    operation, tokenId != null ? maskToken(tokenId) : "N/A");
        }
    }

    /**
     * Logs an inbound TBO API response.
     *
     * @param operation name of the TBO operation
     * @param tokenId   the auth token used (null for auth calls)
     * @param payload   the response body object
     */
    public void logResponse(String operation, String tokenId, Object payload) {
        try {
            log.info("[TBO][{}] <-- RESPONSE | token={} | body={}",
                    operation,
                    tokenId != null ? maskToken(tokenId) : "N/A",
                    objectMapper.writeValueAsString(payload));
        } catch (JsonProcessingException e) {
            log.warn("[TBO][{}] <-- RESPONSE | token={} | body=<serialization error>",
                    operation, tokenId != null ? maskToken(tokenId) : "N/A");
        }
    }

    /** Logs a TBO API error without a response body. */
    public void logError(String operation, String tokenId, String errorMessage) {
        log.error("[TBO][{}] ERROR | token={} | message={}",
                operation,
                tokenId != null ? maskToken(tokenId) : "N/A",
                errorMessage);
    }

    /** Shows only first 8 and last 4 chars of a token to avoid leaking full credentials. */
    private String maskToken(String token) {
        if (token == null || token.length() < 12) return "***";
        return token.substring(0, 8) + "..." + token.substring(token.length() - 4);
    }
}
