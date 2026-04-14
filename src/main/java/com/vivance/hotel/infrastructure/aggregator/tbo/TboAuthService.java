package com.vivance.hotel.infrastructure.aggregator.tbo;

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

import java.time.LocalDate;
import java.time.LocalDateTime;
import java.time.ZoneId;

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

    /**
     * Returns today's valid TBO token.
     * Loads from DB if already authenticated today (IST), otherwise calls TBO auth API.
     *
     * @return the TBO token string ready to be put in the {@code TokenId} header
     */
    public String getValidToken() {
        LocalDate today = LocalDate.now(IST);

        return tokenRepository.findByTokenDate(today)
                .map(token -> {
                    log.info("[TBO] Reusing cached token for date={}", today);
                    return token.getTokenId();
                })
                .orElseGet(() -> authenticate(today));
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
        HttpEntity<TboAuthRequest> entity = new HttpEntity<>(authRequest, headers);

        tboApiLogger.logRequest("TboAuthenticate", null, authRequest);

        try {
            ResponseEntity<TboAuthResponse> response = tboRestTemplate.exchange(
                    cfg.getAuthUrl(),
                    HttpMethod.POST,
                    entity,
                    TboAuthResponse.class
            );

            TboAuthResponse body = response.getBody();
            tboApiLogger.logResponse("TboAuthenticate", null, body);

            if (body != null && body.isSuccess()) {
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

            tboApiLogger.logError("TboAuthenticate", null, errorMsg);
            throw new RuntimeException("TBO authentication failed: " + errorMsg);

        } catch (RuntimeException e) {
            // Re-throw as-is so circuit breaker in TboAggregatorService can react
            throw e;
        } catch (Exception e) {
            log.error("[TBO] Authentication error: {}", e.getMessage(), e);
            throw new RuntimeException("TBO authentication error: " + e.getMessage(), e);
        }
    }
}
