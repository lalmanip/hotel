package com.vivance.hotel.infrastructure.aggregator.tbo.dto;

import com.fasterxml.jackson.annotation.JsonProperty;
import lombok.Data;

import java.math.BigDecimal;
import java.util.List;

/** Response DTO shaped to match TBO API contract. */
@Data
public class TboSearchResponse {

    @JsonProperty("Status")
    private TboStatus status;

    @JsonProperty("Hotels")
    private List<TboHotel> hotels;

    @Data
    public static class TboStatus {
        @JsonProperty("Code")
        private int code;
        @JsonProperty("Description")
        private String description;
    }

    @Data
    public static class TboHotel {
        @JsonProperty("HotelCode")
        private String hotelCode;

        @JsonProperty("HotelName")
        private String hotelName;

        @JsonProperty("CityName")
        private String cityName;

        @JsonProperty("CountryName")
        private String countryName;

        @JsonProperty("Address")
        private String address;

        @JsonProperty("StarRating")
        private int starRating;

        @JsonProperty("MinRate")
        private BigDecimal minRate;

        @JsonProperty("CurrencyCode")
        private String currencyCode;

        @JsonProperty("HotelImageUrl")
        private String hotelImageUrl;

        @JsonProperty("Amenities")
        private List<String> amenities;

        @JsonProperty("Rooms")
        private List<TboRoom> rooms;
    }

    @Data
    public static class TboRoom {
        @JsonProperty("RoomCode")
        private String roomCode;

        @JsonProperty("RoomName")
        private String roomName;

        @JsonProperty("MaxOccupancy")
        private int maxOccupancy;

        @JsonProperty("Rate")
        private BigDecimal rate;

        @JsonProperty("CurrencyCode")
        private String currencyCode;

        @JsonProperty("IsAvailable")
        private boolean isAvailable;
    }
}
