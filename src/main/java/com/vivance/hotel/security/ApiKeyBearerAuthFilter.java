package com.vivance.hotel.security;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.vivance.hotel.dto.response.ApiResponse;
import jakarta.servlet.FilterChain;
import jakarta.servlet.ServletException;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.http.HttpStatus;
import org.springframework.security.authentication.UsernamePasswordAuthenticationToken;
import org.springframework.security.core.authority.AuthorityUtils;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.stereotype.Component;
import org.springframework.util.StringUtils;
import org.springframework.web.filter.OncePerRequestFilter;

import java.io.IOException;

/**
 * Hotel frontend auth gate (same as vivance_api flight flow):
 * requires {@code X-API-KEY} and {@code Authorization: Bearer <Token>}.
 *
 * <p>The Bearer value is the auth_gateway access token JWT and is validated using
 * {@code auth.jwt.secret} (HS256). Subject is treated as userId.
 *
 * <p>On failure, returns HTTP 401 with a standard {@link ApiResponse} error body.
 */
@Slf4j
@Component
@RequiredArgsConstructor
public class ApiKeyBearerAuthFilter extends OncePerRequestFilter {

    private final AuthGatewayJwtService authGatewayJwtService;
    private final ObjectMapper objectMapper;

    @Override
    protected boolean shouldNotFilter(HttpServletRequest request) {
        String uri = normalizePath(request.getRequestURI());
        if (uri == null) return true;
        // Protect all hotel API v1 endpoints (search/prebook/book/getbookingdetails/etc).
        return !uri.startsWith("/api/v1/hotels/");
    }

    @Override
    protected void doFilterInternal(
            HttpServletRequest request,
            HttpServletResponse response,
            FilterChain filterChain
    ) throws ServletException, IOException {

        // If something else already authenticated the request, don't interfere.
        if (SecurityContextHolder.getContext().getAuthentication() != null) {
            filterChain.doFilter(request, response);
            return;
        }

        String apiKey = request.getHeader(VivanceConstants.X_API_KEY);
        String bearerHeader = request.getHeader(VivanceConstants.AUTHORIZATION);

        if (!isValidApiKeyFormat(apiKey) || !StringUtils.hasText(bearerHeader) || !bearerHeader.startsWith(VivanceConstants.BEARER)) {
            denyAuth(response, "Missing or invalid X-API-KEY / Authorization header");
            return;
        }

        String bearerValue = bearerHeader.substring(VivanceConstants.BEARER.length()).trim();
        if (!StringUtils.hasText(bearerValue)) {
            denyAuth(response, "Missing bearer token");
            return;
        }

        // Validate auth_gateway access token JWT
        if (!bearerValue.contains(".") || !authGatewayJwtService.isValid(bearerValue)) {
            denyAuth(response, "Invalid or expired bearer token");
            return;
        }

        String userId = authGatewayJwtService.extractUserId(bearerValue);

        // Minimal "USER" authority for downstream checks.
        UsernamePasswordAuthenticationToken auth =
                new UsernamePasswordAuthenticationToken(userId, bearerValue, AuthorityUtils.createAuthorityList("USER"));
        SecurityContextHolder.getContext().setAuthentication(auth);
        filterChain.doFilter(request, response);
    }

    private static boolean isValidApiKeyFormat(String apiKey) {
        return StringUtils.hasText(apiKey)
                && apiKey.length() >= 36
                && apiKey.startsWith("viv-");
    }

    private void denyAuth(HttpServletResponse response, String message) throws IOException {
        response.setStatus(HttpStatus.UNAUTHORIZED.value());
        response.setContentType("application/json");
        ApiResponse<Void> body = ApiResponse.error(message, "AUTHENTICATION_FAILED");
        response.getWriter().write(objectMapper.writeValueAsString(body));
    }

    /**
     * Normalizes request paths so accidental duplicate slashes (e.g. "//api/...") don't bypass filters.
     */
    private static String normalizePath(String raw) {
        if (raw == null) return null;
        String s = raw;
        while (s.contains("//")) {
            s = s.replace("//", "/");
        }
        return s;
    }
}

