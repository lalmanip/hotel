package com.vivance.hotel.service;

import com.github.benmanes.caffeine.cache.Cache;
import com.github.benmanes.caffeine.cache.Caffeine;
import com.vivance.hotel.config.HotelSearchProperties;
import com.vivance.hotel.dto.response.HotelSearchV1Response;
import org.springframework.stereotype.Component;

@Component
public class HotelSearchV1Cache {

    private final Cache<String, HotelSearchV1Response> cache;

    public HotelSearchV1Cache(HotelSearchProperties props) {
        this.cache = Caffeine.newBuilder()
                .maximumSize(props.getCache().getMaximumSize())
                .expireAfterWrite(props.getCache().getTtl())
                .build();
    }

    public HotelSearchV1Response getIfPresent(String key) {
        return cache.getIfPresent(key);
    }

    public void put(String key, HotelSearchV1Response response) {
        cache.put(key, response);
    }
}

