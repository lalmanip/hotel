package com.vivance.hotel.infrastructure.cache;

import com.fasterxml.jackson.core.type.TypeReference;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.vivance.hotel.dto.request.HotelSearchRequest;
import com.vivance.hotel.dto.response.HotelDetailDto;
import com.vivance.hotel.dto.response.HotelDto;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.data.redis.core.RedisTemplate;
import org.springframework.stereotype.Service;

import java.time.Duration;
import java.util.List;
import java.util.Optional;

/**
 * Redis-backed cache service for hotel data.
 * Using explicit RedisTemplate calls gives full control over TTL per key.
 */
@Slf4j
@Service
@RequiredArgsConstructor
public class HotelCacheService {

    private static final String SEARCH_PREFIX  = "hotel:search:";
    private static final String DETAIL_PREFIX  = "hotel:detail:";
    private static final Duration SEARCH_TTL   = Duration.ofMinutes(10);
    private static final Duration DETAIL_TTL   = Duration.ofMinutes(30);

    private final RedisTemplate<String, Object> redisTemplate;
    private final ObjectMapper objectMapper;

    // ─── Search Cache ─────────────────────────────────────────────────────────

    public Optional<List<HotelDto>> getSearchResults(HotelSearchRequest request) {
        String key = buildSearchKey(request);
        try {
            Object cached = redisTemplate.opsForValue().get(key);
            if (cached != null) {
                List<HotelDto> results = objectMapper.convertValue(cached, new TypeReference<>() {});
                log.debug("Cache HIT for search key={}", key);
                return Optional.of(results);
            }
        } catch (Exception e) {
            log.warn("Cache read error for key={}: {}", key, e.getMessage());
        }
        log.debug("Cache MISS for search key={}", key);
        return Optional.empty();
    }

    public void putSearchResults(HotelSearchRequest request, List<HotelDto> results) {
        String key = buildSearchKey(request);
        try {
            redisTemplate.opsForValue().set(key, results, SEARCH_TTL);
            log.debug("Cached {} search results under key={}", results.size(), key);
        } catch (Exception e) {
            log.warn("Cache write error for key={}: {}", key, e.getMessage());
        }
    }

    // ─── Detail Cache ─────────────────────────────────────────────────────────

    public Optional<HotelDetailDto> getHotelDetail(Long hotelId) {
        String key = DETAIL_PREFIX + hotelId;
        try {
            Object cached = redisTemplate.opsForValue().get(key);
            if (cached != null) {
                HotelDetailDto detail = objectMapper.convertValue(cached, HotelDetailDto.class);
                log.debug("Cache HIT for detail key={}", key);
                return Optional.of(detail);
            }
        } catch (Exception e) {
            log.warn("Cache read error for key={}: {}", key, e.getMessage());
        }
        return Optional.empty();
    }

    public void putHotelDetail(Long hotelId, HotelDetailDto detail) {
        String key = DETAIL_PREFIX + hotelId;
        try {
            redisTemplate.opsForValue().set(key, detail, DETAIL_TTL);
            log.debug("Cached hotel detail under key={}", key);
        } catch (Exception e) {
            log.warn("Cache write error for key={}: {}", key, e.getMessage());
        }
    }

    public void evictHotelDetail(Long hotelId) {
        redisTemplate.delete(DETAIL_PREFIX + hotelId);
    }

    // ─── Helpers ─────────────────────────────────────────────────────────────

    private String buildSearchKey(HotelSearchRequest request) {
        return SEARCH_PREFIX +
               request.getCity().toLowerCase() + ":" +
               request.getCheckIn() + ":" +
               request.getCheckOut() + ":" +
               request.getGuests();
    }
}
