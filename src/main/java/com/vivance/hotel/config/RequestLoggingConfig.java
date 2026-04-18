package com.vivance.hotel.config;

import jakarta.servlet.*;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import lombok.extern.slf4j.Slf4j;
import org.slf4j.MDC;
import org.springframework.core.annotation.Order;
import org.springframework.http.MediaType;
import org.springframework.stereotype.Component;
import org.springframework.web.util.ContentCachingRequestWrapper;
import org.springframework.web.util.ContentCachingResponseWrapper;

import java.io.IOException;
import java.nio.charset.StandardCharsets;
import java.util.*;

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

    @Override
    public void doFilter(ServletRequest servletRequest, ServletResponse servletResponse,
                         FilterChain chain) throws IOException, ServletException {

        HttpServletRequest  request  = (HttpServletRequest)  servletRequest;
        HttpServletResponse response = (HttpServletResponse) servletResponse;

        // Wrap so bodies can be read multiple times
        ContentCachingRequestWrapper  wrappedReq  = new ContentCachingRequestWrapper(request);
        ContentCachingResponseWrapper wrappedResp = new ContentCachingResponseWrapper(response);

        String traceId = UUID.randomUUID().toString().replace("-", "").substring(0, 16);
        MDC.put("traceId", traceId);
        wrappedResp.setHeader("X-Trace-Id", traceId);

        long start = System.currentTimeMillis();

        try {
            chain.doFilter(wrappedReq, wrappedResp);
        } finally {
            long duration = System.currentTimeMillis() - start;
            logRequest(wrappedReq, traceId);
            logResponse(wrappedResp, wrappedReq.getRequestURI(), duration, traceId);
            wrappedResp.copyBodyToResponse();   // must be last — flushes body to client
            MDC.clear();
        }
    }

    // ─── Request logging ──────────────────────────────────────────────────────

    private void logRequest(ContentCachingRequestWrapper req, String traceId) {
        StringBuilder sb = new StringBuilder();
        sb.append("\n╔══════════════════════ INCOMING REQUEST ══════════════════════════");
        sb.append("\n║ TraceId    : ").append(traceId);
        sb.append("\n║ Method     : ").append(req.getMethod());
        sb.append("\n║ URI        : ").append(req.getRequestURI());
        sb.append("\n║ Full URL   : ").append(buildFullUrl(req));
        appendQueryParams(sb, req);
        appendRequestHeaders(sb, req);
        appendBody(sb, "Request Body", req.getContentAsByteArray(), req.getContentType());
        sb.append("\n╚══════════════════════════════════════════════════════════════════");
        log.info(sb.toString());
    }

    private void appendQueryParams(StringBuilder sb, HttpServletRequest req) {
        Map<String, String[]> params = req.getParameterMap();
        if (params.isEmpty()) return;
        sb.append("\n║ Query Params:");
        params.forEach((k, v) -> sb.append("\n║   ").append(k).append(" = ").append(Arrays.toString(v)));
    }

    private void appendRequestHeaders(StringBuilder sb, HttpServletRequest req) {
        sb.append("\n║ Headers:");
        Collections.list(req.getHeaderNames()).forEach(name ->
                sb.append("\n║   ").append(name).append(": ").append(maskIfSensitive(name, req.getHeader(name)))
        );
    }

    // ─── Response logging ─────────────────────────────────────────────────────

    private void logResponse(ContentCachingResponseWrapper resp, String uri, long duration, String traceId) {
        StringBuilder sb = new StringBuilder();
        sb.append("\n╔══════════════════════ OUTGOING RESPONSE ═════════════════════════");
        sb.append("\n║ TraceId    : ").append(traceId);
        sb.append("\n║ URI        : ").append(uri);
        sb.append("\n║ Status     : ").append(resp.getStatus());
        sb.append("\n║ Duration   : ").append(duration).append(" ms");
        appendResponseHeaders(sb, resp);
        appendBody(sb, "Response Body", resp.getContentAsByteArray(), resp.getContentType());
        sb.append("\n╚══════════════════════════════════════════════════════════════════");
        log.info(sb.toString());
    }

    private void appendResponseHeaders(StringBuilder sb, HttpServletResponse resp) {
        sb.append("\n║ Headers:");
        resp.getHeaderNames().forEach(name ->
                sb.append("\n║   ").append(name).append(": ").append(resp.getHeader(name))
        );
    }

    // ─── Shared helpers ───────────────────────────────────────────────────────

    private void appendBody(StringBuilder sb, String label, byte[] body, String contentType) {
        if (body == null || body.length == 0) {
            sb.append("\n║ ").append(label).append(": <empty>");
            return;
        }
        if (!isLoggableContentType(contentType)) {
            sb.append("\n║ ").append(label).append(": <binary — ").append(body.length).append(" bytes>");
            return;
        }
        String bodyStr = new String(body, StandardCharsets.UTF_8);
        if (bodyStr.length() > MAX_BODY_LOG_LENGTH) {
            bodyStr = bodyStr.substring(0, MAX_BODY_LOG_LENGTH) + "... [TRUNCATED]";
        }
        sb.append("\n║ ").append(label).append(":\n").append(indent(bodyStr));
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
                .map(line -> "║   " + line)
                .reduce((a, b) -> a + "\n" + b)
                .orElse("║   " + body);
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
