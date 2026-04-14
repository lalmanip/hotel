package com.vivance.hotel.service;

import com.vivance.hotel.domain.entity.AggregatorMapping;
import com.vivance.hotel.domain.entity.Hotel;
import com.vivance.hotel.domain.enums.AggregatorType;
import com.vivance.hotel.dto.request.HotelSearchRequest;
import com.vivance.hotel.dto.response.HotelDetailDto;
import com.vivance.hotel.dto.response.HotelDto;
import com.vivance.hotel.dto.response.RoomAvailabilityDto;
import com.vivance.hotel.exception.HotelNotFoundException;
import com.vivance.hotel.infrastructure.cache.HotelCacheService;
import com.vivance.hotel.mapper.HotelMapper;
import com.vivance.hotel.repository.AggregatorMappingRepository;
import com.vivance.hotel.repository.HotelRepository;
import com.vivance.hotel.repository.RoomRepository;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.time.temporal.ChronoUnit;
import java.util.List;

@Slf4j
@Service
@RequiredArgsConstructor
public class HotelService {

    private final HotelRepository hotelRepository;
    private final RoomRepository roomRepository;
    private final AggregatorMappingRepository aggregatorMappingRepository;
    private final HotelMapper hotelMapper;
    private final HotelCacheService cacheService;
    private final AggregatorManagerService aggregatorManagerService;

    /**
     * Searches hotels by querying all active aggregators and also the local DB.
     * Results from all sources are merged into a unified list.
     * Uses Redis caching with a 10-minute TTL.
     */
    @Transactional(readOnly = true)
    public List<HotelDto> searchHotels(HotelSearchRequest request) {
        // 1. Check cache first
        return cacheService.getSearchResults(request).orElseGet(() -> {
            log.info("Cache miss — fetching hotels for city={}", request.getCity());

            // 2. Query aggregators (main data source in aggregator-driven model)
            List<HotelDto> results = aggregatorManagerService.searchHotels(request);

            // 3. Supplement with local DB hotels not covered by aggregators
            List<Hotel> localHotels = hotelRepository.findByCityIgnoreCase(request.getCity());
            List<HotelDto> localDtos = hotelMapper.toHotelDtoList(localHotels).stream()
                    .peek(dto -> dto.setStartingPrice(BigDecimal.ZERO))
                    .toList();

            // Merge: aggregator results first, then any local-only hotels
            results.addAll(localDtos);

            // 4. Apply optional filters
            if (request.getMinStarRating() != null) {
                results = results.stream()
                        .filter(h -> h.getStarRating() != null && h.getStarRating() >= request.getMinStarRating())
                        .toList();
            }
            if (request.getMaxPricePerNight() != null) {
                results = results.stream()
                        .filter(h -> h.getStartingPrice() != null &&
                                h.getStartingPrice().doubleValue() <= request.getMaxPricePerNight())
                        .toList();
            }

            // 5. Cache and return
            cacheService.putSearchResults(request, results);
            return results;
        });
    }

    /**
     * Fetches full hotel details. Checks cache, then aggregator, then local DB.
     */
    @Transactional(readOnly = true)
    public HotelDetailDto getHotelDetails(Long hotelId) {
        return cacheService.getHotelDetail(hotelId).orElseGet(() -> {
            // Try to find aggregator mapping for this hotel
            List<AggregatorMapping> mappings = aggregatorMappingRepository.findByHotelId(hotelId);

            HotelDetailDto detail;
            if (!mappings.isEmpty()) {
                AggregatorMapping mapping = mappings.get(0);
                // resultIndex and traceId are not available from DB lookup —
                // caller should use the overloaded getHotelDetails(externalId, resultIndex, traceId)
                // when these are available from the search response.
                detail = aggregatorManagerService.getHotelDetails(
                        mapping.getAggregatorType(), mapping.getExternalHotelId(), null, null);
            } else {
                // Fall back to local DB
                Hotel hotel = hotelRepository.findById(hotelId)
                        .orElseThrow(() -> new HotelNotFoundException(hotelId));
                detail = hotelMapper.toHotelDetailDto(hotel);
            }

            cacheService.putHotelDetail(hotelId, detail);
            return detail;
        });
    }

    /**
     * Fetches hotel details using TBO session context (resultIndex + traceId from search).
     * Use this when the client passes back the search session fields.
     */
    @Transactional(readOnly = true)
    public HotelDetailDto getHotelDetails(
            AggregatorType aggregatorType, String externalHotelId,
            String resultIndex, String traceId) {

        HotelDetailDto detail = aggregatorManagerService.getHotelDetails(
                aggregatorType, externalHotelId, resultIndex, traceId);

        // Cache by externalHotelId hash (no internal hotelId available in this path)
        long cacheKey = externalHotelId.hashCode();
        cacheService.putHotelDetail(cacheKey, detail);
        return detail;
    }

    /**
     * Returns real-time room availability for a hotel and date range.
     */
    @Transactional(readOnly = true)
    public List<RoomAvailabilityDto> getRoomAvailability(
            Long hotelId, LocalDate checkIn, LocalDate checkOut, int guests) {

        long nights = ChronoUnit.DAYS.between(checkIn, checkOut);
        if (nights <= 0) {
            throw new IllegalArgumentException("Check-out must be after check-in");
        }

        // Try aggregator first — no session context available from hotelId lookup, pass nulls
        List<AggregatorMapping> mappings = aggregatorMappingRepository.findByHotelId(hotelId);
        if (!mappings.isEmpty()) {
            AggregatorMapping mapping = mappings.get(0);
            return aggregatorManagerService.getRoomAvailability(
                    mapping.getAggregatorType(), mapping.getExternalHotelId(),
                    null, null, checkIn, checkOut, guests);
        }

        // Fall back to local DB availability query
        return roomRepository.findAvailableRooms(hotelId, checkIn, checkOut, guests).stream()
                .map(room -> {
                    RoomAvailabilityDto dto = RoomAvailabilityDto.builder()
                            .id(room.getId())
                            .roomType(room.getRoomType())
                            .maxOccupancy(room.getMaxOccupancy())
                            .pricePerNight(room.getPricePerNight())
                            .totalPrice(room.getPricePerNight().multiply(BigDecimal.valueOf(nights)))
                            .currency(room.getCurrency())
                            .available(room.isAvailable())
                            .description(room.getDescription())
                            .build();
                    return dto;
                })
                .toList();
    }

    /**
     * Returns real-time room availability using a TBO session context
     * (resultIndex + traceId from the search response).
     * Use this when the client passes back the TBO session fields directly.
     */
    @Transactional(readOnly = true)
    public List<RoomAvailabilityDto> getRoomAvailability(
            AggregatorType aggregatorType, String externalHotelId,
            String resultIndex, String traceId,
            LocalDate checkIn, LocalDate checkOut, int guests) {

        long nights = ChronoUnit.DAYS.between(checkIn, checkOut);
        if (nights <= 0) {
            throw new IllegalArgumentException("Check-out must be after check-in");
        }
        return aggregatorManagerService.getRoomAvailability(
                aggregatorType, externalHotelId, resultIndex, traceId,
                checkIn, checkOut, guests);
    }
}
