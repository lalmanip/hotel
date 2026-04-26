package com.vivance.hotel.service;

import com.github.benmanes.caffeine.cache.Cache;
import com.github.benmanes.caffeine.cache.Caffeine;
import com.vivance.hotel.infrastructure.aggregator.tbo.dto.TboAffiliateSearchResponse;
import org.springframework.stereotype.Component;

import java.time.Duration;

@Component
public class HotelStaticChunkSearchCache {

    private final Cache<String, TboAffiliateSearchResponse> completedSearches = Caffeine.newBuilder()
            .maximumSize(2_000)
            .expireAfterWrite(Duration.ofMinutes(30))
            .build();

    public TboAffiliateSearchResponse getIfPresent(String key) {
        return completedSearches.getIfPresent(key);
    }

    public void putCompleted(String key, TboAffiliateSearchResponse merged) {
        completedSearches.put(key, merged);
    }
}
