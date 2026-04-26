package com.vivance.hotel.config;

import lombok.Data;
import org.springframework.boot.context.properties.ConfigurationProperties;

import java.time.Duration;

@Data
@ConfigurationProperties(prefix = "hotel.search")
public class HotelSearchProperties {

    /**
     * Max hotel codes per TBO search request. TBO typically supports ~100.
     */
    private int tboHotelCodesChunkSize = 100;

    private Cache cache = new Cache();

    @Data
    public static class Cache {
        private long maximumSize = 5_000;
        private Duration ttl = Duration.ofMinutes(5);
    }
}

