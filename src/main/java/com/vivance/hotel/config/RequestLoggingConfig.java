package com.vivance.hotel.config;

import com.vivance.hotel.domain.entity.ApiAccessLog;
import com.vivance.hotel.domain.entity.ApiCallEventLog;
import com.vivance.hotel.service.ApiEventLogService;
import jakarta.servlet.*;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import lombok.extern.slf4j.Slf4j;
import com.fasterxml.jackson.databind.ObjectMapper;
import org.slf4j.MDC;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.core.annotation.Order;
import org.springframework.http.MediaType;
import org.springframework.stereotype.Component;
import org.springframework.web.util.ContentCachingRequestWrapper;
import org.springframework.web.util.ContentCachingResponseWrapper;

import java.io.IOException;
import java.nio.charset.StandardCharsets;
import java.util.*;
import java.util.Date;

/**
 * Comprehensive HTTP request/response logger.
 *
 * <p>Logs for every API call:
 * <ul>
 *   <li>Method, URL, query parameters</li>
 *   <li>All request headers (Authorization value is masked)</li>
 *   <li>Request body (JSON / text)</li>
 *   <li>Response status + headers</li>
 *   <li>Response body (JSON / text)</li>
 *   <li>Total execution time in ms</li>
 * </ul>
 *
 * <p>A unique {@code traceId} is injected into MDC so every log line
 * within one request automatically carries the same ID.
 * The same ID is returned to the caller via the {@code X-Trace-Id} header.
 */
@Slf4j
@Component
@Order(1)
public class RequestLoggingConfig implements Filter {

    /** Paths that carry no interesting bodies — skip body logging for these. */
    private static final Set<String> SKIP_BODY_PATHS = Set.of(
            "/actuator", "/swagger-ui", "/v3/api-docs"
    );

    /** Content types whose body we will log (binary types are excluded). */
    private static final Set<String> LOGGABLE_CONTENT_TYPES = Set.of(
            MediaType.APPLICATION_JSON_VALUE,
            MediaType.APPLICATION_JSON_UTF8_VALUE,
            MediaType.TEXT_PLAIN_VALUE,
            MediaType.TEXT_XML_VALUE,
            MediaType.APPLICATION_XML_VALUE,
            "application/x-www-form-urlencoded"
    );

    private static final int MAX_BODY_LOG_LENGTH = 10_000;
    private static final String TBO_STATIC_ADMIN_PATH = "/api/v1/admin/tbo/static";
    public static final String API_ACCESS_LOG_ATTR = "API_ACCESS_LOG_ENTITY";
    private static final String CLIENT_SERVICE_CHANNEL = "CLIENT-VIVANCE";
    private static final String HOTEL_SEARCH_PATH = "/api/v1/hotels/search";
    private static final String HOTEL_PREBOOK_PATH = "/api/v1/hotels/prebook";
    private static final String HOTEL_BOOK_PATH = "/api/v1/hotels/book";
    private static final String HOTEL_GETBOOKINGDETAILS_PATH = "/api/v1/hotels/getbookingdetails";
    private static final String CLIENT_REQ_EVENT_ID_ATTR = "CLIENT_VIVANCE_REQ_EVENT_ID";

    @Autowired
    private ApiEventLogService apiEventLogService;

    @Autowired
    private ObjectMapper objectMapper;

    @Override
    public void doFilter(ServletRequest servletRequest, ServletResponse servletResponse,
                         FilterChain chain) throws IOException, ServletException {

        HttpServletRequest  request  = (HttpServletRequest)  servletRequest;
        HttpServletResponse response = (HttpServletResponse) servletResponse;

        // Skip API call event logging for TBO static refresh/stats endpoints.
        if (shouldSkipLogging(request)) {
            chain.doFilter(request, response);
            return;
        }

        // Wrap so bodies can be read multiple times.
        // IMPORTANT: set an explicit cache limit; the default can be too small and results in empty bodies.
        ContentCachingRequestWrapper  wrappedReq  = new ContentCachingRequestWrapper(request, MAX_BODY_LOG_LENGTH);
        ContentCachingResponseWrapper wrappedResp = new ContentCachingResponseWrapper(response);

        String traceId = UUID.randomUUID().toString().replace("-", "").substring(0, 16);
        MDC.put("traceId", traceId);
        wrappedResp.setHeader("X-Trace-Id", traceId);

        // Persist one ApiAccessLog row per inbound request so that api_call_event_log rows
        // (written by TboApiLogger for each TBO API call) can reference it via api_access_log_id.
        try {
            ApiAccessLog accessLog = new ApiAccessLog();
            accessLog.setCreatedDatetime(new Date());
            accessLog.setModule(resolveModule(request.getRequestURI()));
            accessLog.setUrlOrAction(request.getRequestURI());
            accessLog.setIpAddress(request.getRemoteAddr());
            accessLog.setUserSessionId(traceId);
            ApiAccessLog saved = apiEventLogService.saveApiAccessLog(accessLog);
            wrappedReq.setAttribute(API_ACCESS_LOG_ATTR, saved);
        } catch (Exception e) {
            log.warn("Failed to save ApiAccessLog: {}", e.getMessage());
        }

        // Save CLIENT request event early so ordering is:
        // CLIENT request -> TBO request -> TBO response -> CLIENT response
        saveClientRequestEvent(wrappedReq);

        long start = System.currentTimeMillis();

        try {
            chain.doFilter(wrappedReq, wrappedResp);
        } finally {
            long duration = System.currentTimeMillis() - start;
            logRequest(wrappedReq, traceId);
            logResponse(wrappedResp, wrappedReq.getRequestURI(), duration, traceId);
            // Now that the request body has been consumed by MVC, the wrapper contains cached bytes.
            // Update the already-inserted CLIENT request row so content is not empty.
            updateClientRequestEventContent(wrappedReq);
            saveClientResponseEvent(wrappedReq, wrappedResp, duration);
            wrappedResp.copyBodyToResponse();   // must be last — flushes body to client
            MDC.clear();
        }
    }

    private String resolveModule(String uri) {
        if (uri == null) return "hotel";
        String[] parts = uri.split("/");
        // /api/v1/hotels/... → parts[3] = "hotels"
        return parts.length > 3 ? parts[3] : "hotel";
    }

    private boolean shouldSkipLogging(HttpServletRequest request) {
        String uri = request.getRequestURI();
        return uri != null && uri.startsWith(TBO_STATIC_ADMIN_PATH);
    }

    private void saveClientRequestEvent(ContentCachingRequestWrapper req) {
        try {
            String uri = req.getRequestURI();
            if (!isClientTrackedPath(uri)) {
                return;
            }

            ApiAccessLog accessLog = (ApiAccessLog) req.getAttribute(API_ACCESS_LOG_ATTR);
            Long accessLogId = accessLog != null ? accessLog.getId() : null;

            String headersJson = objectMapper.writeValueAsString(extractRequestHeaders(req));
            String parametersJson = objectMapper.writeValueAsString(Map.of(
                    "url", buildFullUrl(req)
            ));

            String reqBody = safeBody(req.getContentAsByteArray(), req.getContentType());
            ApiCallEventLog requestEvent = new ApiCallEventLog();
            requestEvent.setApiAccessLogId(accessLogId);
            requestEvent.setServiceChannel(CLIENT_SERVICE_CHANNEL);
            requestEvent.setEventName(resolveClientEventName(uri));
            requestEvent.setEventType("REQUEST");
            requestEvent.setHeaders(headersJson);
            requestEvent.setParameters(parametersJson);
            requestEvent.setResultToken(null);
            // Body may not be cached yet (ContentCachingRequestWrapper fills after MVC reads it).
            // We'll update this row later in finally with the real body.
            requestEvent.setContent(reqBody);
            requestEvent.setCreatedDatetime(new Date());
            apiEventLogService.saveOrUpdateApiCallEvent(requestEvent);
            if (requestEvent.getId() != null) {
                req.setAttribute(CLIENT_REQ_EVENT_ID_ATTR, requestEvent.getId());
            }
        } catch (Exception e) {
            log.warn("Failed saving CLIENT-VIVANCE api_call_event_log entry: {}", e.getMessage());
        }
    }

    private void updateClientRequestEventContent(ContentCachingRequestWrapper req) {
        try {
            String uri = req.getRequestURI();
            if (!isClientTrackedPath(uri)) {
                return;
            }
            Object idObj = req.getAttribute(CLIENT_REQ_EVENT_ID_ATTR);
            if (!(idObj instanceof Long id)) {
                return;
            }
            String body = safeBody(req.getContentAsByteArray(), req.getContentType());
            // Only update if we actually captured something meaningful now.
            if (body == null || body.isBlank() || "<empty>".equals(body)) {
                return;
            }
            apiEventLogService.updateApiCallEventContent(id, body);
        } catch (Exception e) {
            log.warn("Failed updating CLIENT-VIVANCE request content: {}", e.getMessage());
        }
    }

    private void saveClientResponseEvent(ContentCachingRequestWrapper req,
                                         ContentCachingResponseWrapper resp,
                                         long durationMs) {
        try {
            String uri = req.getRequestURI();
            if (!isClientTrackedPath(uri)) {
                return;
            }

            ApiAccessLog accessLog = (ApiAccessLog) req.getAttribute(API_ACCESS_LOG_ATTR);
            Long accessLogId = accessLog != null ? accessLog.getId() : null;

            String respHeadersJson = objectMapper.writeValueAsString(extractResponseHeaders(resp));
            String respParamsJson = objectMapper.writeValueAsString(Map.of(
                    "url", buildFullUrl(req),
                    "durationMs", durationMs,
                    "httpStatus", resp.getStatus()
            ));
            String respBody = safeBody(resp.getContentAsByteArray(), resp.getContentType());

            ApiCallEventLog responseEvent = new ApiCallEventLog();
            responseEvent.setApiAccessLogId(accessLogId);
            responseEvent.setServiceChannel(CLIENT_SERVICE_CHANNEL);
            responseEvent.setEventName(resolveClientEventName(uri));
            responseEvent.setEventType("RESPONSE");
            responseEvent.setHeaders(respHeadersJson);
            responseEvent.setParameters(respParamsJson);
            responseEvent.setResultToken(null);
            responseEvent.setContent(respBody);
            responseEvent.setCreatedDatetime(new Date());
            apiEventLogService.saveOrUpdateApiCallEvent(responseEvent);
        } catch (Exception e) {
            log.warn("Failed saving CLIENT-VIVANCE api_call_event_log entry: {}", e.getMessage());
        }
    }

    private boolean isClientTrackedPath(String uri) {
        return uri != null && (HOTEL_SEARCH_PATH.equals(uri)
                || HOTEL_PREBOOK_PATH.equals(uri)
                || HOTEL_BOOK_PATH.equals(uri)
                || HOTEL_GETBOOKINGDETAILS_PATH.equals(uri));
    }

    private String resolveClientEventName(String uri) {
        if (HOTEL_PREBOOK_PATH.equals(uri)) return "ClientHotelPreBook";
        if (HOTEL_BOOK_PATH.equals(uri)) return "ClientHotelBook";
        if (HOTEL_GETBOOKINGDETAILS_PATH.equals(uri)) return "ClientHotelGetBookingDetail";
        return "ClientHotelSearch";
    }

    private Map<String, String> extractRequestHeaders(HttpServletRequest req) {
        Map<String, String> headers = new LinkedHashMap<>();
        Collections.list(req.getHeaderNames()).forEach(name ->
                headers.put(name, maskIfSensitive(name, req.getHeader(name))));
        return headers;
    }

    private Map<String, String> extractResponseHeaders(HttpServletResponse resp) {
        Map<String, String> headers = new LinkedHashMap<>();
        resp.getHeaderNames().forEach(name -> headers.put(name, resp.getHeader(name)));
        return headers;
    }

    private String safeBody(byte[] bodyBytes, String contentType) {
        String body = "<empty>";
        if (bodyBytes != null && bodyBytes.length > 0) {
            body = new String(bodyBytes, StandardCharsets.UTF_8);
        }
        if (body.length() > MAX_BODY_LOG_LENGTH) {
            body = body.substring(0, MAX_BODY_LOG_LENGTH) + "\n... [TRUNCATED]";
        }
        return body;
    }

    // ─── Request logging ──────────────────────────────────────────────────────

    private void logRequest(ContentCachingRequestWrapper req, String traceId) {
        StringBuilder sb = new StringBuilder();
        sb.append("\n -------- INCOMING REQUEST ---------");
        sb.append("\n TraceId    : ").append(traceId);
        sb.append("\n Method     : ").append(req.getMethod());
        sb.append("\n URI        : ").append(req.getRequestURI());
        sb.append("\n Full URL   : ").append(buildFullUrl(req));
        appendQueryParams(sb, req);
        appendRequestHeaders(sb, req);
        appendBody(sb, "Request Body", req.getContentAsByteArray(), req.getContentType());
        sb.append("\n-----------------------------");
        log.info(sb.toString());
    }

    private void appendQueryParams(StringBuilder sb, HttpServletRequest req) {
        Map<String, String[]> params = req.getParameterMap();
        if (params.isEmpty()) return;
        sb.append("\n Query Params:");
        params.forEach((k, v) -> sb.append("\n   ").append(k).append(" = ").append(Arrays.toString(v)));
    }

    private void appendRequestHeaders(StringBuilder sb, HttpServletRequest req) {
        sb.append("\n Headers:");
        Collections.list(req.getHeaderNames()).forEach(name ->
                sb.append("\n   ").append(name).append(": ").append(maskIfSensitive(name, req.getHeader(name)))
        );
    }

    // ─── Response logging ─────────────────────────────────────────────────────

    private void logResponse(ContentCachingResponseWrapper resp, String uri, long duration, String traceId) {
        StringBuilder sb = new StringBuilder();
        sb.append("\n-------- OUTGOING RESPONSE --------");
        sb.append("\n TraceId    : ").append(traceId);
        sb.append("\n URI        : ").append(uri);
        sb.append("\n Status     : ").append(resp.getStatus());
        sb.append("\n Duration   : ").append(duration).append(" ms");
        appendResponseHeaders(sb, resp);
        appendBody(sb, "Response Body", resp.getContentAsByteArray(), resp.getContentType());
        sb.append("\n-----------------------------");
        log.info(sb.toString());
    }

    private void appendResponseHeaders(StringBuilder sb, HttpServletResponse resp) {
        sb.append("\n Headers:");
        resp.getHeaderNames().forEach(name ->
                sb.append("\n   ").append(name).append(": ").append(resp.getHeader(name))
        );
    }

    // ─── Shared helpers ───────────────────────────────────────────────────────

    private void appendBody(StringBuilder sb, String label, byte[] body, String contentType) {
        if (body == null || body.length == 0) {
            sb.append("\n ").append(label).append(": <empty>");
            return;
        }
        if (!isLoggableContentType(contentType)) {
            sb.append("\n ").append(label).append(": <binary — ").append(body.length).append(" bytes>");
            return;
        }
        String bodyStr = new String(body, StandardCharsets.UTF_8);
        if (bodyStr.length() > MAX_BODY_LOG_LENGTH) {
            bodyStr = bodyStr.substring(0, MAX_BODY_LOG_LENGTH) + "... [TRUNCATED]";
        }
        sb.append("\n ").append(label).append(":\n").append(indent(bodyStr));
    }

    private boolean isLoggableContentType(String contentType) {
        if (contentType == null) return true;   // log unknown types by default
        String ct = contentType.toLowerCase();
        return LOGGABLE_CONTENT_TYPES.stream().anyMatch(ct::contains);
    }

    private String buildFullUrl(HttpServletRequest req) {
        StringBuilder url = new StringBuilder(req.getRequestURL());
        String query = req.getQueryString();
        if (query != null) url.append('?').append(query);
        return url.toString();
    }

    /** Indents each line of the body for readability inside the log box. */
    private String indent(String body) {
        return Arrays.stream(body.split("\n"))
                .map(line -> "   " + line)
                .reduce((a, b) -> a + "\n" + b)
                .orElse("   " + body);
    }

    /** Masks sensitive header values — shows first 8 chars then ****. */
    private String maskIfSensitive(String headerName, String value) {
        if (headerName == null || value == null) return value;
        String lower = headerName.toLowerCase();
        if (lower.equals("authorization") || lower.equals("x-api-key") || lower.equals("cookie")) {
            if (value.length() <= 8) return "****";
            return value.substring(0, 8) + "****";
        }
        return value;
    }
}
