package com.vivance.hotel.service;

import com.vivance.hotel.domain.entity.AggregatorMapping;
import com.vivance.hotel.domain.entity.Hotel;
import com.vivance.hotel.domain.enums.AggregatorType;
import com.vivance.hotel.dto.request.HotelSearchRequest;
import com.vivance.hotel.dto.response.HotelDetailDto;
import com.vivance.hotel.dto.response.HotelDto;
import com.vivance.hotel.dto.response.RoomAvailabilityDto;
import com.vivance.hotel.infrastructure.aggregator.tbo.dto.TboAffiliatePreBookResponse;
import com.vivance.hotel.infrastructure.aggregator.tbo.dto.TboAffiliateBookRequest;
import com.vivance.hotel.infrastructure.aggregator.tbo.dto.TboBookResponse;
import com.vivance.hotel.infrastructure.aggregator.tbo.dto.TboAffiliateSearchResponse;
import com.vivance.hotel.infrastructure.aggregator.tbo.dto.TboGetBookingDetailRequest;
import com.vivance.hotel.infrastructure.aggregator.tbo.dto.TboGetBookingDetailResponse;
import com.vivance.hotel.exception.HotelNotFoundException;
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
import java.util.ArrayList;
import java.util.List;

@Slf4j
@Service
@RequiredArgsConstructor
public class HotelService {

    private final HotelRepository hotelRepository;
    private final RoomRepository roomRepository;
    private final AggregatorMappingRepository aggregatorMappingRepository;
    private final HotelMapper hotelMapper;
    private final AggregatorManagerService aggregatorManagerService;
    private final ApiSessionItineraryService apiSessionItineraryService;

    /**
     * Searches hotels by querying all active aggregators and also the local DB.
     * Results from all sources are merged into a unified list.
     */
    @Transactional(readOnly = true)
    public List<HotelDto> searchHotels(HotelSearchRequest request) {
        log.info("Fetching hotels for city={}, hotelCodes={}", request.getCity(), request.getHotelCodes());

        List<HotelDto> results = new ArrayList<>(aggregatorManagerService.searchHotels(request));

        if (request.getCity() != null && !request.getCity().isBlank()) {
            List<Hotel> localHotels = hotelRepository.findByCityIgnoreCase(request.getCity());
            List<HotelDto> localDtos = hotelMapper.toHotelDtoList(localHotels).stream()
                    .peek(dto -> dto.setStartingPrice(BigDecimal.ZERO))
                    .toList();
            results.addAll(localDtos);
        }

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

        return results;
    }

    /**
     * Raw passthrough search used by the frontend for TBO affiliate inventory.
     */
    @Transactional(readOnly = true)
    public TboAffiliateSearchResponse searchHotelsRawTbo(HotelSearchRequest request) {
        return aggregatorManagerService.searchHotelsRawTbo(request);
    }

    @Transactional(readOnly = true)
    public TboAffiliatePreBookResponse preBookRawTbo(String bookingCode, String paymentMode) {
        // Backwards compatible method: no userSessionId so we can't persist itinerary.
        return aggregatorManagerService.preBookRawTbo(bookingCode, paymentMode);
    }

    @Transactional
    public TboAffiliatePreBookResponse preBookRawTboAndRecordItinerary(
            String traceId, String bookingCode, String paymentMode) {
        TboAffiliatePreBookResponse resp = aggregatorManagerService.preBookRawTbo(bookingCode, paymentMode);
        apiSessionItineraryService.recordPreBook(traceId, bookingCode, resp);
        return resp;
    }

    @Transactional(readOnly = true)
    public TboBookResponse bookRawTbo(TboAffiliateBookRequest request) {
        return aggregatorManagerService.bookRawTbo(request);
    }

    @Transactional(readOnly = true)
    public TboGetBookingDetailResponse getBookingDetailRawTbo(TboGetBookingDetailRequest request) {
        return aggregatorManagerService.getBookingDetailRawTbo(request);
    }

    /**
     * Fetches full hotel details from the aggregator when mapped, otherwise from the local DB.
     */
    @Transactional(readOnly = true)
    public HotelDetailDto getHotelDetails(Long hotelId) {
        List<AggregatorMapping> mappings = aggregatorMappingRepository.findByHotelId(hotelId);

        if (!mappings.isEmpty()) {
            AggregatorMapping mapping = mappings.get(0);
            return aggregatorManagerService.getHotelDetails(
                    mapping.getAggregatorType(), mapping.getExternalHotelId(), null, null);
        }

        Hotel hotel = hotelRepository.findById(hotelId)
                .orElseThrow(() -> new HotelNotFoundException(hotelId));
        return hotelMapper.toHotelDetailDto(hotel);
    }

    /**
     * Fetches hotel details using TBO session context (resultIndex + traceId from search).
     * Use this when the client passes back the search session fields.
     */
    @Transactional(readOnly = true)
    public HotelDetailDto getHotelDetails(
            AggregatorType aggregatorType, String externalHotelId,
            String resultIndex, String traceId) {

        return aggregatorManagerService.getHotelDetails(
                aggregatorType, externalHotelId, resultIndex, traceId);
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

        List<AggregatorMapping> mappings = aggregatorMappingRepository.findByHotelId(hotelId);
        if (!mappings.isEmpty()) {
            AggregatorMapping mapping = mappings.get(0);
            return aggregatorManagerService.getRoomAvailability(
                    mapping.getAggregatorType(), mapping.getExternalHotelId(),
                    null, null, checkIn, checkOut, guests);
        }

        return roomRepository.findAvailableRooms(hotelId, checkIn, checkOut, guests).stream()
                .map(room -> RoomAvailabilityDto.builder()
                        .id(room.getId())
                        .roomType(room.getRoomType())
                        .maxOccupancy(room.getMaxOccupancy())
                        .pricePerNight(room.getPricePerNight())
                        .totalPrice(room.getPricePerNight().multiply(BigDecimal.valueOf(nights)))
                        .currency(room.getCurrency())
                        .available(room.isAvailable())
                        .description(room.getDescription())
                        .build())
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
