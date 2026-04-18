package com.vivance.hotel.infrastructure.aggregator.tbo;

import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.databind.SerializationFeature;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;

/**
 * Structured logger for all outbound TBO API calls.
 * Prints request and response in a box-drawing format matching RequestLoggingConfig,
 * so both the frontend ↔ hotel-backend and hotel-backend ↔ TBO traffic are easy to read
 * in the same log stream.
 */
@Slf4j
@Service
@RequiredArgsConstructor
public class TboApiLogger {

    private final ObjectMapper objectMapper;

    private static final int MAX_BODY_LOG_LENGTH = 10_000;

    /**
     * Logs an outbound TBO API request.
     *
     * @param operation name of the TBO operation (e.g. "TboHotelSearch")
     * @param url       full URL being called
     * @param tokenId   the auth token used (null for auth calls)
     * @param payload   the request body object
     */
    public void logRequest(String operation, String url, String tokenId, Object payload) {
        String body = toJson(payload);
        StringBuilder sb = new StringBuilder();
        sb.append("\n╔══════════════════════ TBO OUTGOING REQUEST ══════════════════════");
        sb.append("\n║ Operation  : ").append(operation);
        sb.append("\n║ URL        : POST ").append(url);
        sb.append("\n║ Token      : ").append(tokenId != null ? maskToken(tokenId) : "N/A (auth call)");
        sb.append("\n║ Request Body:");
        sb.append(indent(body));
        sb.append("\n╚══════════════════════════════════════════════════════════════════");
        log.info(sb.toString());
    }

    /**
     * Logs an inbound TBO API response.
     *
     * @param operation name of the TBO operation
     * @param url       full URL that was called
     * @param tokenId   the auth token used (null for auth calls)
     * @param payload   the response body object
     */
    public void logResponse(String operation, String url, String tokenId, Object payload) {
        String body = toJson(payload);
        StringBuilder sb = new StringBuilder();
        sb.append("\n╔══════════════════════ TBO INCOMING RESPONSE ═════════════════════");
        sb.append("\n║ Operation  : ").append(operation);
        sb.append("\n║ URL        : ").append(url);
        sb.append("\n║ Token      : ").append(tokenId != null ? maskToken(tokenId) : "N/A (auth call)");
        sb.append("\n║ Response Body:");
        sb.append(indent(body));
        sb.append("\n╚══════════════════════════════════════════════════════════════════");
        log.info(sb.toString());
    }

    /** Logs a TBO API error. */
    public void logError(String operation, String url, String tokenId, String errorMessage) {
        StringBuilder sb = new StringBuilder();
        sb.append("\n╔══════════════════════ TBO ERROR ═════════════════════════════════");
        sb.append("\n║ Operation  : ").append(operation);
        if (url != null) sb.append("\n║ URL        : ").append(url);
        sb.append("\n║ Token      : ").append(tokenId != null ? maskToken(tokenId) : "N/A");
        sb.append("\n║ Error      : ").append(errorMessage);
        sb.append("\n╚══════════════════════════════════════════════════════════════════");
        log.error(sb.toString());
    }

    // ─── Helpers ─────────────────────────────────────────────────────────────

    private String toJson(Object obj) {
        if (obj == null) return "<null>";
        try {
            ObjectMapper pretty = objectMapper.copy()
                    .enable(SerializationFeature.INDENT_OUTPUT);
            String json = pretty.writeValueAsString(obj);
            if (json.length() > MAX_BODY_LOG_LENGTH) {
                json = json.substring(0, MAX_BODY_LOG_LENGTH) + "\n... [TRUNCATED]";
            }
            return json;
        } catch (JsonProcessingException e) {
            return "<serialization error: " + e.getMessage() + ">";
        }
    }

    private String indent(String body) {
        StringBuilder sb = new StringBuilder();
        for (String line : body.split("\n")) {
            sb.append("\n║   ").append(line);
        }
        return sb.toString();
    }

    /** Shows only first 8 and last 4 chars of a token to avoid leaking full credentials. */
    private String maskToken(String token) {
        if (token == null || token.length() < 12) return "***";
        return token.substring(0, 8) + "..." + token.substring(token.length() - 4);
    }
}
