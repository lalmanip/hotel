package com.vivance.hotel.security;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.vivance.hotel.dto.response.ApiResponse;
import io.jsonwebtoken.JwtException;
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
import org.springframework.web.filter.OncePerRequestFilter;

import java.io.IOException;

/**
 * Second-pass gate (inside the Spring Security filter chain):
 * Reads the {@link JwtAuthToken} placed by {@link ApiKeyBearerAuthFilter},
 * validates the JWT via {@link AuthGatewayJwtService}, extracts the userId (sub),
 * and sets an authenticated {@link UsernamePasswordAuthenticationToken} in the
 * {@link SecurityContextHolder}.
 *
 * <p>If no session attribute is present the request is for a public endpoint —
 * the filter passes through without blocking. Returns HTTP 403 for invalid or
 * expired JWTs.
 *
 * <p>Not a {@code @Component}: instantiated in {@link com.vivance.hotel.config.SecurityConfig}
 * to prevent Spring Boot auto-registering it as a duplicate servlet filter.
 */
@Slf4j
@RequiredArgsConstructor
public class JwtAuthenticationFilter extends OncePerRequestFilter {

    private final AuthGatewayJwtService jwtService;
    private final ObjectMapper objectMapper;

    @Override
    protected void doFilterInternal(HttpServletRequest request, HttpServletResponse response,
            FilterChain filterChain) throws ServletException, IOException {

        JwtAuthToken authToken = (JwtAuthToken) request.getAttribute(ApiKeyBearerAuthFilter.SESSION_ATTR);

        // No attribute → public endpoint (ApiKeyBearerAuthFilter skipped it)
        if (authToken == null) {
            filterChain.doFilter(request, response);
            return;
        }

        String token = authToken.getToken();
        String userId;
        try {
            userId = jwtService.extractUserId(token);
        } catch (JwtException | IllegalArgumentException e) {
            // TEMPORARY — log full reason for debugging; remove once token issue is resolved
            log.warn("[AUTH] JWT rejected for {} — {}: {}", request.getRequestURI(),
                    e.getClass().getSimpleName(), e.getMessage());
            deny(response, "Invalid or expired JWT token");
            return;
        }
        authToken.setUserSessionId(userId);
        request.setAttribute(ApiKeyBearerAuthFilter.SESSION_ATTR, authToken);

        UsernamePasswordAuthenticationToken authentication =
                new UsernamePasswordAuthenticationToken(
                        userId, authToken, AuthorityUtils.createAuthorityList("USER"));
        SecurityContextHolder.getContext().setAuthentication(authentication);

        filterChain.doFilter(request, response);
    }

    private void deny(HttpServletResponse response, String message) throws IOException {
        response.setStatus(HttpStatus.FORBIDDEN.value());
        response.setContentType("application/json");
        response.getWriter().write(objectMapper.writeValueAsString(
                ApiResponse.error(message, "AUTHENTICATION_FAILED")));
    }
}
