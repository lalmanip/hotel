package com.vivance.hotel.dto.request;

import jakarta.validation.constraints.*;
import lombok.Data;

import java.time.LocalDate;

@Data
public class HotelSearchRequest {

    @NotBlank(message = "City is required")
    private String city;

    @NotNull(message = "Check-in date is required")
    @FutureOrPresent(message = "Check-in date must be today or in the future")
    private LocalDate checkIn;

    @NotNull(message = "Check-out date is required")
    @Future(message = "Check-out date must be in the future")
    private LocalDate checkOut;

    @NotNull(message = "Number of guests is required")
    @Min(value = 1, message = "At least 1 guest is required")
    @Max(value = 20, message = "Maximum 20 guests allowed per booking")
    private Integer guests;

    /** Optional: filter by star rating */
    @Min(1) @Max(5)
    private Integer minStarRating;

    /** Optional: max price per night */
    @DecimalMin("0.0")
    private Double maxPricePerNight;

    /** Optional: specific aggregator to query. Null means use all active aggregators. */
    private String aggregator;

    /**
     * TBO-specific: numeric city identifier (e.g. "130443" for New Delhi).
     * Required when aggregator is TBO. Look up city IDs from TBO's city master.
     */
    private String cityId;

    /**
     * ISO country code (e.g. "IN"). Defaults to "IN" if not supplied.
     */
    private String countryCode;

    /**
     * ISO nationality code for guests (e.g. "IN"). Defaults to "IN".
     */
    private String guestNationality;

    /** Number of rooms required. Defaults to 1. */
    @Min(1)
    private Integer noOfRooms;
}
