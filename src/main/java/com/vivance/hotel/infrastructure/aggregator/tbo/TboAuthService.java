package com.vivance.hotel.infrastructure.aggregator.tbo;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.vivance.hotel.config.AggregatorProperties;
import com.vivance.hotel.domain.entity.TboAuthToken;
import com.vivance.hotel.infrastructure.aggregator.tbo.dto.TboAuthRequest;
import com.vivance.hotel.infrastructure.aggregator.tbo.dto.TboAuthResponse;
import com.vivance.hotel.repository.TboAuthTokenRepository;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.http.*;
import org.springframework.stereotype.Service;
import org.springframework.web.client.RestTemplate;

import org.springframework.transaction.annotation.Propagation;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDate;
import java.time.LocalDateTime;
import java.time.ZoneId;
import java.util.List;
import java.util.Optional;

/**
 * Manages TBO authentication tokens.
 *
 * <p><b>Strategy:</b> TBO issues one token per calendar day (Asia/Kolkata time).
 * We persist the token in the {@code tbo_auth_tokens} table so the application
 * authenticates at most once per day — any subsequent call within the same day
 * reuses the stored token. When the day rolls past midnight IST the old token
 * is automatically ignored and a fresh authentication is performed.
 *
 * <p>Adapted from the Vivance Flight aggregator microservice with the following changes:
 * <ul>
 *   <li>Log4j → SLF4J via {@code @Slf4j}</li>
 *   <li>Config wired through {@link AggregatorProperties} instead of bare {@code @Value}</li>
 *   <li>Constructor injection kept — no field injection</li>
 * </ul>
 */
@Slf4j
@Service
@RequiredArgsConstructor
public class TboAuthService {

    private static final ZoneId IST = ZoneId.of("Asia/Kolkata");

    private final RestTemplate tboRestTemplate;
    private final TboAuthTokenRepository tokenRepository;
    private final TboApiLogger tboApiLogger;
    private final AggregatorProperties aggregatorProperties;
    private final ObjectMapper objectMapper;

    /**
     * Returns today's valid TBO token (IST calendar day).
     * Checks {@code tbo_auth_token} first — calls TBO Authenticate API only when no
     * token exists for today. Persists the new token immediately in its own transaction
     * so it is available to all subsequent calls within the same day.
     *
     * {@code REQUIRES_NEW} suspends any surrounding read-only transaction (e.g. from
     * HotelService) so the INSERT is always flushed and committed to the DB.
     */
    @Transactional(propagation = Propagation.REQUIRES_NEW)
    public String getValidToken() {
        LocalDate today = LocalDate.now(IST);

        return tokenRepository.findByTokenDate(today)
                .map(token -> {
                    log.info("[TBO] Reusing cached token for date={}", today);
                    return token.getTokenId();
                })
                .orElseGet(() -> authenticate(today));
    }

    /**
     * Validates a TokenId received from the frontend against the DB cache.
     * Token is considered valid only for the current IST calendar date.
     */
    @Transactional(readOnly = true)
    public boolean isFrontendTokenValid(String tokenId) {
        if (tokenId == null || tokenId.isBlank()) return false;
        LocalDate today = LocalDate.now(IST);
        return tokenRepository.findByTokenDate(today)
                .map(t -> tokenId.equals(t.getTokenId()))
                .orElse(false);
    }

    // ─── Private ─────────────────────────────────────────────────────────────

    private String authenticate(LocalDate tokenDate) {
        log.info("[TBO] No token found for date={}, performing authentication...", tokenDate);

        AggregatorProperties.TboConfig cfg = aggregatorProperties.getTbo();

        TboAuthRequest authRequest = new TboAuthRequest(
                cfg.getClientId(),
                cfg.getUserName(),
                cfg.getPassword(),
                cfg.getEndUserIp()
        );

        HttpHeaders headers = new HttpHeaders();
        headers.setContentType(MediaType.APPLICATION_JSON);
        headers.setAccept(List.of(MediaType.ALL));
        HttpEntity<TboAuthRequest> entity = new HttpEntity<>(authRequest, headers);

        tboApiLogger.logRequest("TboAuthenticate", cfg.getAuthUrl(), null, authRequest);

        try {
            ResponseEntity<String> raw = doAuthenticateCall(cfg.getAuthUrl(), entity);
            String rawBody = raw.getBody();

            if (rawBody == null || rawBody.isBlank()) {
                tboApiLogger.logError("TboAuthenticate", cfg.getAuthUrl(), null, "Empty auth response body");
                throw new RuntimeException("TBO authentication failed: empty response body");
            }

            String trimmed = rawBody.trim();
            if (!looksLikeJson(trimmed)) {
                // Common when the resource path is wrong (WCF responds with text/plain like "Invalid Resource Requested")
                String msg = "Non-JSON auth response (HTTP " + raw.getStatusCode().value() + "): " + preview(trimmed);
                tboApiLogger.logError("TboAuthenticate", cfg.getAuthUrl(), null, msg);

                // Retry once with the alternate trailing-slash form (some WCF routers treat these differently)
                String retryUrl = cfg.getAuthUrl().endsWith("/")
                        ? cfg.getAuthUrl().substring(0, cfg.getAuthUrl().length() - 1)
                        : cfg.getAuthUrl() + "/";

                log.warn("[TBO] Auth returned non-JSON; retrying with alternate URL: {}", retryUrl);
                ResponseEntity<String> raw2 = doAuthenticateCall(retryUrl, entity);
                String body2 = Optional.ofNullable(raw2.getBody()).orElse("").trim();
                if (!body2.isBlank() && looksLikeJson(body2)) {
                    TboAuthResponse parsed = objectMapper.readValue(body2, TboAuthResponse.class);
                    tboApiLogger.logResponse("TboAuthenticate", retryUrl, null, parsed);
                    return persistTokenOrThrow(tokenDate, retryUrl, parsed);
                }

                String msg2 = "Non-JSON auth response after retry (HTTP " + raw2.getStatusCode().value() + "): " + preview(body2);
                tboApiLogger.logError("TboAuthenticate", retryUrl, null, msg2);

                throw new RuntimeException("TBO authentication failed: " + msg);
            }

            TboAuthResponse body = objectMapper.readValue(trimmed, TboAuthResponse.class);
            tboApiLogger.logResponse("TboAuthenticate", cfg.getAuthUrl(), null, body);

            return persistTokenOrThrow(tokenDate, cfg.getAuthUrl(), body);

        } catch (RuntimeException e) {
            // Re-throw as-is so circuit breaker in TboAggregatorService can react
            throw e;
        } catch (Exception e) {
            log.error("[TBO] Authentication error: {}", e.getMessage(), e);
            throw new RuntimeException("TBO authentication error: " + e.getMessage(), e);
        }
    }

    private ResponseEntity<String> doAuthenticateCall(String url, HttpEntity<TboAuthRequest> entity) {
        return tboRestTemplate.exchange(url, HttpMethod.POST, entity, String.class);
    }

    private String persistTokenOrThrow(LocalDate tokenDate, String url, TboAuthResponse body) {
        if (body != null && body.isSuccess() && body.getTokenId() != null && !body.getTokenId().isBlank()) {
            TboAuthToken saved = tokenRepository.save(
                    TboAuthToken.builder()
                            .tokenId(body.getTokenId())
                            .tokenDate(tokenDate)
                            .createdAt(LocalDateTime.now(IST))
                            .build()
            );
            log.info("[TBO] New token persisted for date={}", tokenDate);
            return saved.getTokenId();
        }

        String errorMsg = (body != null && body.getError() != null)
                ? body.getError().getErrorMessage()
                : "Unknown authentication error";

        tboApiLogger.logError("TboAuthenticate", url, null, errorMsg);
        throw new RuntimeException("TBO authentication failed: " + errorMsg);
    }

    private static boolean looksLikeJson(String s) {
        return s.startsWith("{") || s.startsWith("[");
    }

    private static String preview(String s) {
        if (s == null) return "<null>";
        String oneLine = s.replace("\r", "").replace("\n", " ");
        return oneLine.length() > 300 ? oneLine.substring(0, 300) + "... [TRUNCATED]" : oneLine;
    }
}
