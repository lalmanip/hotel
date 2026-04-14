package com.vivance.hotel.config;

import com.vivance.hotel.domain.enums.AggregatorType;
import lombok.Data;
import org.springframework.boot.context.properties.ConfigurationProperties;

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
        /** e.g. https://HotelBE.tektravels.com/internalhotelservice.svc/rest */
        private String internalBaseUrl;
        private int timeoutSeconds = 10;

        // ── Endpoint paths (relative to baseUrl unless noted) ─────────────
        private String searchPath        = "/Gethotelresult";
        private String hotelInfoPath     = "/GetHotelInfo";
        private String getRoomPath       = "/GetHotelRoom";
        private String blockPath         = "/blockRoom";
        private String bookPath          = "/book";
        /** Relative to internalBaseUrl */
        private String bookingDetailPath = "/GetBookingDetail";

        // ── Authentication ────────────────────────────────────────────────
        private String authUrl;
        private String clientId;
        private String userName;
        private String password;
        /** Your server's outbound IP, sent to TBO as EndUserIp */
        private String endUserIp;
    }
}
