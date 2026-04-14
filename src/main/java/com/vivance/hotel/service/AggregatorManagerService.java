package com.vivance.hotel.service;

import com.vivance.hotel.config.AggregatorProperties;
import com.vivance.hotel.domain.enums.AggregatorType;
import com.vivance.hotel.dto.request.HotelSearchRequest;
import com.vivance.hotel.dto.response.BookingDto;
import com.vivance.hotel.dto.response.HotelDetailDto;
import com.vivance.hotel.dto.response.HotelDto;
import com.vivance.hotel.dto.response.RoomAvailabilityDto;
import com.vivance.hotel.infrastructure.aggregator.AggregatorFactory;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;

import java.time.LocalDate;
import java.util.List;
import java.util.concurrent.CompletableFuture;
import java.util.stream.Collectors;

/**
 * Orchestrates calls across multiple active aggregators.
 * Handles parallel fan-out for search and routes single-aggregator calls
 * for hotel detail, room availability, and booking.
 */
@Slf4j
@Service
@RequiredArgsConstructor
public class AggregatorManagerService {

    private final AggregatorFactory aggregatorFactory;
    private final AggregatorProperties aggregatorProperties;

    /** Fans out to all active aggregators in parallel, merges results. */
    public List<HotelDto> searchHotels(HotelSearchRequest request) {
        List<AggregatorType> types = resolveActiveAggregators(request.getAggregator());
        log.info("Searching {} aggregator(s): {}", types.size(), types);

        if (types.size() == 1) {
            return aggregatorFactory.getAggregator(types.get(0)).searchHotels(request);
        }

        List<CompletableFuture<List<HotelDto>>> futures = types.stream()
                .map(type -> CompletableFuture.supplyAsync(() -> {
                    try {
                        return aggregatorFactory.getAggregator(type).searchHotels(request);
                    } catch (Exception e) {
                        log.error("Aggregator {} failed during search: {}", type, e.getMessage());
                        return List.<HotelDto>of();
                    }
                }))
                .toList();

        return futures.stream()
                .map(CompletableFuture::join)
                .flatMap(List::stream)
                .collect(Collectors.toList());
    }

    /**
     * Fetches hotel details from the specified aggregator.
     * {@code resultIndex} and {@code traceId} are the TBO session values from search.
     */
    public HotelDetailDto getHotelDetails(
            AggregatorType aggregatorType, String externalHotelId,
            String resultIndex, String traceId) {

        return aggregatorFactory.getAggregator(aggregatorType)
                .getHotelDetails(externalHotelId, resultIndex, traceId);
    }

    /**
     * Gets room availability from the specified aggregator.
     * {@code resultIndex} and {@code traceId} are the TBO session values from search.
     */
    public List<RoomAvailabilityDto> getRoomAvailability(
            AggregatorType aggregatorType,
            String externalHotelId,
            String resultIndex,
            String traceId,
            LocalDate checkIn,
            LocalDate checkOut,
            int guests) {

        return aggregatorFactory.getAggregator(aggregatorType)
                .getRoomAvailability(externalHotelId, resultIndex, traceId, checkIn, checkOut, guests);
    }

    /** Delegates booking (block + confirm) to the specified aggregator. */
    public BookingDto bookHotel(
            AggregatorType aggregatorType,
            String externalHotelId,
            String externalRoomId,
            LocalDate checkIn,
            LocalDate checkOut,
            int guests,
            String guestName,
            String guestEmail,
            String resultIndex,
            String traceId,
            String roomTypeCode,
            String ratePlanCode,
            String guestNationality) {

        return aggregatorFactory.getAggregator(aggregatorType).bookHotel(
                externalHotelId, externalRoomId,
                checkIn, checkOut, guests,
                guestName, guestEmail,
                resultIndex, traceId,
                roomTypeCode, ratePlanCode, guestNationality
        );
    }

    private List<AggregatorType> resolveActiveAggregators(String requestedAggregator) {
        if (requestedAggregator != null && !requestedAggregator.isBlank()) {
            return List.of(AggregatorType.valueOf(requestedAggregator.toUpperCase()));
        }
        return aggregatorProperties.getActiveAggregatorTypes();
    }
}
