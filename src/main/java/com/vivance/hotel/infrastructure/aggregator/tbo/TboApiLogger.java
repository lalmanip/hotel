package com.vivance.hotel.infrastructure.aggregator.tbo;

import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.databind.SerializationFeature;
import com.vivance.hotel.config.RequestLoggingConfig;
import com.vivance.hotel.domain.entity.ApiAccessLog;
import com.vivance.hotel.domain.entity.ApiCallEventLog;
import com.vivance.hotel.service.ApiEventLogService;
import jakarta.servlet.http.HttpServletRequest;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.http.HttpHeaders;
import org.springframework.stereotype.Service;
import org.springframework.web.context.request.RequestContextHolder;
import org.springframework.web.context.request.ServletRequestAttributes;

import java.util.Date;
import java.util.LinkedHashMap;
import java.util.Map;

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
    private final ApiEventLogService apiEventLogService;

    private static final int MAX_BODY_LOG_LENGTH = 10_000;
    private static final String SERVICE_CHANNEL = "VIVANCE-TBO";

    /**
     * Logs an outbound TBO API request.
     *
     * @param operation name of the TBO operation (e.g. "TboAffiliateSearch")
     * @param url       full URL being called
     * @param tokenId   the auth token used (null for auth calls)
     * @param payload   the request body object
     */
    public void logRequest(String operation, String url, String tokenId, Object payload) {
        logRequest(operation, url, tokenId, null, payload);
    }

    /**
     * Logs an outbound TBO API request including HTTP headers.
     */
    public void logRequest(String operation, String url, String tokenId, HttpHeaders headers, Object payload) {
        String body = toJson(payload);
        String headerJson = headers != null ? toJson(headers) : "<null>";
        StringBuilder sb = new StringBuilder();
        sb.append("\n╔══════════════════════ TBO OUTGOING REQUEST ══════════════════════");
        sb.append("\n║ Operation  : ").append(operation);
        sb.append("\n║ URL        : POST ").append(url);
        sb.append("\n║ Token      : ").append(tokenId != null ? maskToken(tokenId) : "N/A (auth call)");
        sb.append("\n║ Headers:");
        sb.append(indent(headerJson));
        sb.append("\n║ Request Body:");
        sb.append(indent(body));
        sb.append("\n╚══════════════════════════════════════════════════════════════════");
        log.info(sb.toString());
        saveEvent(operation, "REQUEST", url, tokenId, headerJson, body, null);
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
        logResponse(operation, url, tokenId, null, payload);
    }

    /**
     * Logs an inbound TBO API response including HTTP headers.
     */
    public void logResponse(String operation, String url, String tokenId, HttpHeaders headers, Object payload) {
        String body = toJson(payload);
        String headerJson = headers != null ? toJson(headers) : "<null>";
        StringBuilder sb = new StringBuilder();
        sb.append("\n╔══════════════════════ TBO INCOMING RESPONSE ═════════════════════");
        sb.append("\n║ Operation  : ").append(operation);
        sb.append("\n║ URL        : ").append(url);
        sb.append("\n║ Token      : ").append(tokenId != null ? maskToken(tokenId) : "N/A (auth call)");
        sb.append("\n║ Headers:");
        sb.append(indent(headerJson));
        sb.append("\n║ Response Body:");
        sb.append(indent(body));
        sb.append("\n╚══════════════════════════════════════════════════════════════════");
        log.info(sb.toString());
        saveEvent(operation, "RESPONSE", url, tokenId, headerJson, body, null);
    }

    /**
     * Logs an inbound TBO API response using the raw JSON string (preserves all fields exactly
     * as returned by TBO, no re-serialization through Jackson).
     */
    public void logRawJsonResponse(String operation, String url, String tokenId,
                                   int httpStatus, HttpHeaders headers, String rawJson) {
        String body = prettyPrintJson(rawJson);
        String headerJson = headers != null ? toJson(headers) : "<null>";
        StringBuilder sb = new StringBuilder();
        sb.append("\n╔══════════════════════ TBO INCOMING RESPONSE ═════════════════════");
        sb.append("\n║ Operation  : ").append(operation);
        sb.append("\n║ URL        : ").append(url);
        sb.append("\n║ HTTP Status: ").append(httpStatus);
        sb.append("\n║ Token      : ").append(tokenId != null ? maskToken(tokenId) : "N/A (auth call)");
        sb.append("\n║ Headers:");
        sb.append(indent(headerJson));
        sb.append("\n║ Response Body:");
        sb.append(indent(body));
        sb.append("\n╚══════════════════════════════════════════════════════════════════");
        log.info(sb.toString());
        saveEvent(operation, "RESPONSE", url, tokenId, headerJson, body, null);
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
        saveEvent(operation, "ERROR", url, tokenId, null, errorMessage, errorMessage);
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

    private String prettyPrintJson(String raw) {
        if (raw == null) return "<null>";
        try {
            Object node = objectMapper.readTree(raw);
            ObjectMapper pretty = objectMapper.copy().enable(SerializationFeature.INDENT_OUTPUT);
            String formatted = pretty.writeValueAsString(node);
            if (formatted.length() > MAX_BODY_LOG_LENGTH) {
                formatted = formatted.substring(0, MAX_BODY_LOG_LENGTH) + "\n... [TRUNCATED]";
            }
            return formatted;
        } catch (Exception e) {
            return raw.length() > MAX_BODY_LOG_LENGTH ? raw.substring(0, MAX_BODY_LOG_LENGTH) + "\n... [TRUNCATED]" : raw;
        }
    }

    private String indent(String body) {
        StringBuilder sb = new StringBuilder();
        for (String line : body.split("\n")) {
            sb.append("\n║   ").append(line);
        }
        return sb.toString();
    }

    private void saveEvent(
            String operation,
            String eventType,
            String url,
            String tokenId,
            String outboundHeaders,
            String content,
            String errorMessage
    ) {
        try {
            HttpServletRequest currentRequest = getCurrentRequest();

            ApiCallEventLog entry = new ApiCallEventLog();
            entry.setCreatedDatetime(new Date());
            entry.setServiceChannel(SERVICE_CHANNEL);
            entry.setEventName(operation);
            entry.setEventType(eventType);
            entry.setResultToken(tokenId);
            entry.setContent(content);
            entry.setHeaders(resolveHeadersJson(currentRequest, outboundHeaders));
            entry.setParameters(buildParameters(url, errorMessage));

            if (currentRequest != null) {
                ApiAccessLog accessLog = (ApiAccessLog) currentRequest.getAttribute(
                        RequestLoggingConfig.API_ACCESS_LOG_ATTR);
                if (accessLog != null) {
                    entry.setApiAccessLogId(accessLog.getId());
                }
            }

            apiEventLogService.saveOrUpdateApiCallEvent(entry);
        } catch (Exception e) {
            log.error("Failed saving TBO api_call_event_log entry (operation={}, eventType={})",
                    operation, eventType, e);
        }
    }

    private String resolveHeadersJson(HttpServletRequest currentRequest, String outboundHeaders)
            throws JsonProcessingException {
        if (currentRequest == null) {
            return outboundHeaders;
        }

        Map<String, String> requestHeaders = new LinkedHashMap<>();
        currentRequest.getHeaderNames().asIterator().forEachRemaining(name -> {
            String raw = currentRequest.getHeader(name);
            String value = "authorization".equalsIgnoreCase(name) && raw != null && raw.length() > 10
                    ? raw.substring(0, 10) + "***"
                    : raw;
            requestHeaders.put(name, value);
        });

        if (outboundHeaders != null && !outboundHeaders.isBlank() && !"<null>".equals(outboundHeaders)) {
            requestHeaders.put("tboOutboundHeaders", outboundHeaders);
        }
        return objectMapper.writeValueAsString(requestHeaders);
    }

    private String buildParameters(String url, String errorMessage) throws JsonProcessingException {
        Map<String, Object> parameters = new LinkedHashMap<>();
        parameters.put("url", url);
        if (errorMessage != null && !errorMessage.isBlank()) {
            parameters.put("errorMessage", errorMessage);
        }
        return objectMapper.writeValueAsString(parameters);
    }

    private HttpServletRequest getCurrentRequest() {
        try {
            return ((ServletRequestAttributes) RequestContextHolder.currentRequestAttributes()).getRequest();
        } catch (Exception e) {
            return null;
        }
    }

    /** Shows only first 8 and last 4 chars of a token to avoid leaking full credentials. */
    private String maskToken(String token) {
        if (token == null || token.length() < 12) return "***";
        return token.substring(0, 8) + "..." + token.substring(token.length() - 4);
    }
}
