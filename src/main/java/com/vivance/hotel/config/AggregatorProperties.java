package com.vivance.hotel.config;

import com.vivance.hotel.domain.enums.AggregatorType;
import lombok.Data;
import org.springframework.boot.context.properties.ConfigurationProperties;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;

/**
 * Binds aggregator configuration from application.yml under {@code hotel.aggregators}.
 */
@Data
@ConfigurationProperties(prefix = "hotel.aggregators")
public class AggregatorProperties {

    /** Comma-separated list of active aggregators, e.g. "TBO" or "TBO,EXPEDIA" */
    private String active = "TBO";

    private TboConfig tbo = new TboConfig();

    /**
     * Returns the list of active aggregator types parsed from the YAML value.
     */
    public List<AggregatorType> getActiveAggregatorTypes() {
        return Arrays.stream(active.split(","))
                .map(String::trim)
                .map(String::toUpperCase)
                .map(AggregatorType::valueOf)
                .toList();
    }

    @Data
    public static class TboConfig {
        // ── Base URLs ─────────────────────────────────────────────────────
        /** e.g. https://HotelBE.tektravels.com/hotelservice.svc/rest */
        private String baseUrl;
        private int timeoutSeconds = 10;

        // ── Endpoint paths (relative to baseUrl) ──────────────────────────
        private String bookPath          = "/book";
        /** Relative to {@link #baseUrl} (same host as Book), e.g. /Getbookingdetail */
        private String bookingDetailPath = "/Getbookingdetail";

        // ── Authentication ────────────────────────────────────────────────
        private String authUrl;
        private String clientId;
        private String userName;
        private String password;
        /** Your server's outbound IP, sent to TBO as EndUserIp */
        private String endUserIp;

        // ── Static data API (basic auth) ──────────────────────────────────
        private String staticBaseUrl = "http://api.tbotechnology.in/TBOHolidays_HotelAPI";
        private String staticUserName = "TBOStaticAPITest";
        private String staticPassword = "Tbo@11530818";
        /** Comma-separated {@code Hotelcodes} per Hoteldetails call (TBO allows up to 50). */
        private int staticHotelDetailsBatchSize = 50;
        /** Max concurrent Hoteldetails HTTP calls (TBO allows 10 parallel). */
        private int staticHotelDetailsMaxConcurrentRequests = 10;
        /**
         * When non-empty, only rows in {@code tbo_hotels_static} whose {@code city_name} matches
         * one of these values (case-insensitive trim) are considered for sync. Empty = all cities (production).
         */
        private List<String> staticHotelDetailsRestrictToCityNames = new ArrayList<>();

        // ── Affiliate hotel flow URLs (Search -> PreBook) ───────────────────
        private String affiliateSearchUrl = "https://affiliate.tektravels.com/HotelAPI/Search";
        private String affiliatePreBookUrl = "https://affiliate.tektravels.com/HotelAPI/PreBook";
    }
}
