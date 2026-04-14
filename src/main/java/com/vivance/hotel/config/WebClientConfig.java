package com.vivance.hotel.config;

import io.netty.channel.ChannelOption;
import io.netty.handler.timeout.ReadTimeoutHandler;
import io.netty.handler.timeout.WriteTimeoutHandler;
import lombok.RequiredArgsConstructor;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.http.HttpHeaders;
import org.springframework.http.MediaType;
import org.springframework.http.client.reactive.ReactorClientHttpConnector;
import org.springframework.web.reactive.function.client.ExchangeFilterFunction;
import org.springframework.web.reactive.function.client.WebClient;
import reactor.core.publisher.Mono;
import reactor.netty.http.client.HttpClient;

import org.springframework.web.client.RestTemplate;

import java.time.Duration;
import java.util.concurrent.TimeUnit;

import lombok.extern.slf4j.Slf4j;

@Slf4j
@Configuration
@RequiredArgsConstructor
public class WebClientConfig {

    private final AggregatorProperties aggregatorProperties;

    /**
     * WebClient pre-configured for TBO API calls.
     * Wire this into {@link com.vivance.hotel.infrastructure.aggregator.tbo.TboAggregatorService}
     * when integrating the real TBO REST API.
     */
    @Bean("tboWebClient")
    public WebClient tboWebClient() {
        int timeoutSeconds = aggregatorProperties.getTbo().getTimeoutSeconds();

        HttpClient httpClient = HttpClient.create()
                .option(ChannelOption.CONNECT_TIMEOUT_MILLIS, timeoutSeconds * 1000)
                .responseTimeout(Duration.ofSeconds(timeoutSeconds))
                .doOnConnected(conn -> conn
                        .addHandlerLast(new ReadTimeoutHandler(timeoutSeconds, TimeUnit.SECONDS))
                        .addHandlerLast(new WriteTimeoutHandler(timeoutSeconds, TimeUnit.SECONDS)));

        return WebClient.builder()
                .baseUrl(aggregatorProperties.getTbo().getBaseUrl())
                .defaultHeader(HttpHeaders.CONTENT_TYPE, MediaType.APPLICATION_JSON_VALUE)
                // TBO auth uses a per-request TokenId field in the JSON body, not an API key header
                .clientConnector(new ReactorClientHttpConnector(httpClient))
                .filter(logRequest())
                .filter(logResponse())
                .build();
    }

    /**
     * RestTemplate used by {@link com.vivance.hotel.infrastructure.aggregator.tbo.TboAuthService}.
     * Kept separate from WebClient so the auth service mirrors the existing Flight microservice pattern.
     */
    @Bean
    public RestTemplate tboRestTemplate() {
        return new RestTemplate();
    }

    /** Generic WebClient for other integrations. */
    @Bean("genericWebClient")
    public WebClient genericWebClient() {
        return WebClient.builder()
                .defaultHeader(HttpHeaders.CONTENT_TYPE, MediaType.APPLICATION_JSON_VALUE)
                .filter(logRequest())
                .build();
    }

    private ExchangeFilterFunction logRequest() {
        return ExchangeFilterFunction.ofRequestProcessor(req -> {
            log.debug("[WebClient] --> {} {}", req.method(), req.url());
            return Mono.just(req);
        });
    }

    private ExchangeFilterFunction logResponse() {
        return ExchangeFilterFunction.ofResponseProcessor(resp -> {
            log.debug("[WebClient] <-- HTTP {}", resp.statusCode());
            return Mono.just(resp);
        });
    }
}
