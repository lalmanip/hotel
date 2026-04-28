package com.vivance.hotel.security;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.vivance.hotel.dto.response.ApiResponse;
import jakarta.servlet.FilterChain;
import jakarta.servlet.ServletException;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.core.Ordered;
import org.springframework.core.annotation.Order;
import org.springframework.http.HttpStatus;
import org.springframework.stereotype.Component;
import org.springframework.util.StringUtils;
import org.springframework.web.filter.OncePerRequestFilter;

import java.io.IOException;

/**
 * First-pass gate (servlet-level, runs before Spring Security):
 * - Requires {@code X-API-KEY} (starts with "viv-", length >= 36)
 * - Requires {@code Authorization: Bearer <JWT>}
 *
 * <p>Extracts both values into a {@link JwtAuthToken} request attribute
 * ({@link #SESSION_ATTR}) for downstream JWT validation in {@link JwtAuthenticationFilter}.
 * Returns HTTP 403 immediately on any header violation.
 *
 * <p>Public endpoints (Swagger, auth, health, admin static, GET hotel searches)
 * are skipped entirely.
 */
@Slf4j
@Component
@Order(Ordered.HIGHEST_PRECEDENCE)
@RequiredArgsConstructor
public class ApiKeyBearerAuthFilter extends OncePerRequestFilter {

    public static final String SESSION_ATTR = "VIV_HOTEL_SESSION";

    private final ObjectMapper objectMapper;

    @Override
    protected boolean shouldNotFilter(HttpServletRequest request) {
        String uri = normalizePath(request.getRequestURI());
        if (uri == null) return true;
        String method = request.getMethod();
        return uri.startsWith("/api/v1/auth/")
                || uri.startsWith("/v3/api-docs")
                || uri.startsWith("/swagger-ui")
                || uri.equals("/swagger-ui.html")
                || uri.startsWith("/actuator/health")
                || uri.startsWith("/api/v1/admin/tbo/static/")
                || ("GET".equalsIgnoreCase(method) && (uri.startsWith("/api/hotels/") || uri.startsWith("/api/v1/hotels/")));
    }

    @Override
    protected void doFilterInternal(HttpServletRequest request, HttpServletResponse response,
            FilterChain filterChain) throws ServletException, IOException {

        String apiKey = request.getHeader(VivanceConstants.X_API_KEY);
        String authHeader = request.getHeader(VivanceConstants.AUTHORIZATION);

        if (!isValidApiKey(apiKey)) {
            log.warn("[AUTH] Missing or invalid X-API-KEY from {}", request.getRemoteAddr());
            deny(response, "Missing or invalid X-API-KEY");
            return;
        }

        if (!StringUtils.hasText(authHeader) || !authHeader.startsWith(VivanceConstants.BEARER)) {
            log.warn("[AUTH] Missing Authorization header from {}", request.getRemoteAddr());
            deny(response, "Missing Authorization header");
            return;
        }

        String bearerValue = authHeader.substring(VivanceConstants.BEARER.length()).trim();
        if (!looksLikeJwt(bearerValue)) {
            log.warn("[AUTH] Bearer value is not a JWT from {}", request.getRemoteAddr());
            deny(response, "Authorization token must be a JWT");
            return;
        }

        JwtAuthToken authToken = new JwtAuthToken();
        authToken.setApiKey(apiKey);
        authToken.setToken(bearerValue);
        authToken.setIpAddress(request.getRemoteAddr());
        request.setAttribute(SESSION_ATTR, authToken);

        filterChain.doFilter(request, response);
    }

    private static boolean isValidApiKey(String apiKey) {
        return StringUtils.hasText(apiKey) && apiKey.length() >= 36 && apiKey.startsWith("viv-");
    }

    private static boolean looksLikeJwt(String token) {
        if (!StringUtils.hasText(token)) return false;
        int dots = 0;
        for (int i = 0; i < token.length(); i++) {
            if (token.charAt(i) == '.') dots++;
            if (dots > 2) return false;
        }
        return dots == 2;
    }

    private void deny(HttpServletResponse response, String message) throws IOException {
        response.setStatus(HttpStatus.FORBIDDEN.value());
        response.setContentType("application/json");
        response.getWriter().write(objectMapper.writeValueAsString(
                ApiResponse.error(message, "AUTHENTICATION_FAILED")));
    }

    private static String normalizePath(String raw) {
        if (raw == null) return null;
        String s = raw;
        while (s.contains("//")) s = s.replace("//", "/");
        return s;
    }
}
