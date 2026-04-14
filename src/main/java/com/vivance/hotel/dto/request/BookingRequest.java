package com.vivance.hotel.dto.request;

import jakarta.validation.constraints.*;
import lombok.Data;

import java.time.LocalDate;

@Data
public class BookingRequest {

    @NotNull(message = "Hotel ID is required")
    private Long hotelId;

    @NotNull(message = "Room ID is required")
    private Long roomId;

    @NotNull(message = "Check-in date is required")
    @FutureOrPresent(message = "Check-in date must be today or in the future")
    private LocalDate checkIn;

    @NotNull(message = "Check-out date is required")
    @Future(message = "Check-out date must be in the future")
    private LocalDate checkOut;

    @NotNull(message = "Number of guests is required")
    @Min(value = 1, message = "At least 1 guest required")
    @Max(value = 20, message = "Maximum 20 guests")
    private Integer guests;

    @Size(max = 200, message = "Special requests must be under 200 characters")
    private String specialRequests;

    // ── TBO session fields (from search → room availability responses) ────────

    /** TBO TraceId from the search response. Required for TBO bookings. */
    private String traceId;

    /** TBO ResultIndex from the search response. Required for TBO bookings. */
    private String resultIndex;

    /** TBO HotelCode (externalHotelId) from the search response. */
    private String externalHotelId;

    /** TBO RoomTypeCode from the room availability response. */
    private String roomTypeCode;

    /** TBO RatePlanCode from the room availability response. */
    private String ratePlanCode;

    /** TBO RoomIndex from the room availability response. */
    private String tboRoomIndex;

    /** ISO guest nationality code, e.g. "IN". Defaults to IN if not supplied. */
    private String guestNationality;
}
