package com.vivance.hotel.infrastructure.aggregator.tbo.dto;

import com.fasterxml.jackson.annotation.JsonProperty;
import lombok.Data;

import java.util.List;

@Data
public class TboStaticHotelCodeListResponse {

    @JsonProperty("Status")
    private TboStaticStatus status;

    @JsonProperty("Hotels")
    private List<HotelItem> hotels;

    @Data
    public static class HotelItem {
        @JsonProperty("HotelCode")
        private String hotelCode;

        @JsonProperty("HotelName")
        private String hotelName;

        @JsonProperty("Latitude")
        private String latitude;

        @JsonProperty("Longitude")
        private String longitude;

        @JsonProperty("HotelRating")
        private String hotelRating;

        @JsonProperty("Address")
        private String address;

        @JsonProperty("CountryName")
        private String countryName;

        @JsonProperty("CountryCode")
        private String countryCode;

        @JsonProperty("CityName")
        private String cityName;
    }
}

