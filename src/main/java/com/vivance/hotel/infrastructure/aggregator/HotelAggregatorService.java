package com.vivance.hotel.infrastructure.aggregator;

import com.vivance.hotel.domain.enums.AggregatorType;
import com.vivance.hotel.dto.request.HotelSearchRequest;
import com.vivance.hotel.dto.response.BookingDto;
import com.vivance.hotel.dto.response.HotelDetailDto;
import com.vivance.hotel.dto.response.HotelDto;
import com.vivance.hotel.dto.response.RoomAvailabilityDto;

import java.time.LocalDate;
import java.util.List;

/**
 * Common aggregator contract. Every hotel supplier must implement this interface.
 *
 * <p><b>Session context parameters ({@code resultIndex}, {@code traceId}):</b>
 * TBO requires a {@code ResultIndex} and {@code TraceId} (returned from search) to be
 * echoed in every subsequent call within the same booking session.
 * Other aggregators may pass {@code null} for these parameters and ignore them.
 */
public interface HotelAggregatorService {

    /** Returns the aggregator type handled by this implementation. */
    AggregatorType getAggregatorType();

    /**
     * Step 2 — Search hotels matching the request criteria.
     * The returned {@link HotelDto} objects carry aggregator-specific session fields
     * ({@code traceId}, {@code resultIndex}) that must flow through to subsequent calls.
     */
    List<HotelDto> searchHotels(HotelSearchRequest request);

    /**
     * Step 3 — Fetch full hotel details (images, facilities, description).
     *
     * @param externalHotelId aggregator hotel code (e.g. TBO HotelCode)
     * @param resultIndex     aggregator result index from search (null for non-TBO)
     * @param traceId         session trace ID from search (null for non-TBO)
     */
    HotelDetailDto getHotelDetails(String externalHotelId, String resultIndex, String traceId);

    /**
     * Step 4 — Get real-time room availability with pricing and cancellation policies.
     *
     * @param externalHotelId aggregator hotel code
     * @param resultIndex     aggregator result index from search (null for non-TBO)
     * @param traceId         session trace ID from search (null for non-TBO)
     * @param checkIn         check-in date
     * @param checkOut        check-out date
     * @param guests          number of guests
     */
    List<RoomAvailabilityDto> getRoomAvailability(
            String externalHotelId,
            String resultIndex,
            String traceId,
            LocalDate checkIn,
            LocalDate checkOut,
            int guests
    );

    /**
     * Step 5+6 — Block room, then confirm booking with the aggregator.
     * TBO performs Block (price-lock) first, then Book in a single call to this method.
     *
     * @param externalHotelId  aggregator hotel code
     * @param externalRoomId   room index or room ID from availability response
     * @param checkIn          check-in date
     * @param checkOut         check-out date
     * @param guests           number of guests
     * @param guestName        primary guest full name
     * @param guestEmail       primary guest email
     * @param resultIndex      TBO result index from search (null for non-TBO)
     * @param traceId          TBO trace ID from search (null for non-TBO)
     * @param roomTypeCode     TBO room type code from room availability (null for non-TBO)
     * @param ratePlanCode     TBO rate plan code from room availability (null for non-TBO)
     * @param guestNationality ISO country code of the guest (e.g. "IN")
     * @return booking confirmation DTO with {@code aggregatorBookingId} populated
     */
    BookingDto bookHotel(
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
            String guestNationality
    );
}
