package com.vivance.hotel.dto.request;

import com.fasterxml.jackson.annotation.JsonProperty;
import jakarta.validation.Valid;
import jakarta.validation.constraints.*;
import lombok.Data;

import java.time.LocalDate;
import java.util.List;

@Data
public class HotelSearchRequest {

    @NotNull(message = "Check-in date is required")
    @FutureOrPresent(message = "Check-in date must be today or in the future")
    @JsonProperty("CheckIn")
    private LocalDate checkIn;

    @NotNull(message = "Check-out date is required")
    @Future(message = "Check-out date must be in the future")
    @JsonProperty("CheckOut")
    private LocalDate checkOut;

    /** City name for local DB search. Optional when HotelCodes is supplied. */
    @JsonProperty("City")
    private String city;

    /**
     * Total guest count. Optional when PaxRooms is supplied — derived automatically
     * as the sum of Adults across all rooms.
     */
    @Min(value = 1, message = "At least 1 guest is required")
    @Max(value = 20, message = "Maximum 20 guests allowed per booking")
    @JsonProperty("Guests")
    private Integer guests;

    @JsonProperty("GuestNationality")
    private String guestNationality;

    /** TBO HotelCode(s), comma-separated. */
    @JsonProperty("HotelCodes")
    private String hotelCodes;

    /** ISO country code, e.g. "IN". */
    @JsonProperty("CountryCode")
    private String countryCode;

    /** TBO numeric city identifier. */
    @JsonProperty("CityId")
    private String cityId;

    /** Specific aggregator to query. Null means all active aggregators. */
    @JsonProperty("Aggregator")
    private String aggregator;

    @Min(1) @Max(5)
    @JsonProperty("MinStarRating")
    private Integer minStarRating;

    @DecimalMin("0.0")
    @JsonProperty("MaxPricePerNight")
    private Double maxPricePerNight;

    @Min(1)
    @JsonProperty("NoOfRooms")
    private Integer noOfRooms;

    /** Max seconds to wait for aggregator responses. */
    @JsonProperty("ResponseTime")
    private Double responseTime;

    @JsonProperty("IsDetailedResponse")
    private Boolean isDetailedResponse;

    @Valid
    @JsonProperty("PaxRooms")
    private List<PaxRoom> paxRooms;

    @JsonProperty("Filters")
    private Filters filters;

    /**
     * Optional: If the client already has a valid TBO token (TokenId), it can be provided here.
     * Backend will use it for affiliate Search calls and skip Authenticate.
     */
    @JsonProperty("TokenId")
    private String tokenId;

    /**
     * Optional: client-provided EndUserIp for TBO affiliate Search.
     * If absent, backend will use configured {@code hotel.aggregators.tbo.end-user-ip}.
     */
    @JsonProperty("EndUserIp")
    private String endUserIp;

    /** Returns effective guest count: explicit value or sum of Adults across PaxRooms. */
    public int effectiveGuests() {
        if (guests != null) return guests;
        if (paxRooms != null && !paxRooms.isEmpty()) {
            return paxRooms.stream().mapToInt(r -> r.adults != null ? r.adults : 1).sum();
        }
        return 1;
    }

    @Data
    public static class PaxRoom {
        @JsonProperty("Adults")
        private Integer adults;

        @JsonProperty("Children")
        private Integer children;

        @JsonProperty("ChildrenAges")
        private List<Integer> childrenAges;
    }

    @Data
    public static class Filters {
        @JsonProperty("Refundable")
        private Boolean refundable;

        @JsonProperty("NoOfRooms")
        private Integer noOfRooms;

        @JsonProperty("MealType")
        private String mealType;

        @JsonProperty("StarRating")
        private String starRating;
    }
}
