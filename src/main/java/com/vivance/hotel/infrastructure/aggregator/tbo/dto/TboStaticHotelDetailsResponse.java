package com.vivance.hotel.infrastructure.aggregator.tbo.dto;

import com.fasterxml.jackson.annotation.JsonIgnoreProperties;
import com.fasterxml.jackson.annotation.JsonProperty;
import com.fasterxml.jackson.databind.JsonNode;
import lombok.Data;

import java.util.List;

@Data
public class TboStaticHotelDetailsResponse {

    @JsonProperty("Status")
    private TboStaticStatus status;

    @JsonProperty("HotelDetails")
    private List<HotelDetailItem> hotelDetails;

    @Data
    @JsonIgnoreProperties(ignoreUnknown = true)
    public static class HotelDetailItem {

        @JsonProperty("HotelCode")
        private String hotelCode;

        @JsonProperty("HotelName")
        private String hotelName;

        @JsonProperty("Description")
        private String description;

        @JsonProperty("HotelFacilities")
        private List<String> hotelFacilities;

        /** Free-form map/object from TBO; keys may include odd spacing. */
        @JsonProperty("Attractions")
        private JsonNode attractions;

        @JsonProperty("Image")
        private String image;

        @JsonProperty("RoomID")
        private List<String> roomId;

        @JsonProperty("Images")
        private List<String> images;

        @JsonProperty("Email")
        private String email;

        @JsonProperty("HotelWebsiteUrl")
        private String hotelWebsiteUrl;

        @JsonProperty("HotelFees")
        private JsonNode hotelFees;

        @JsonProperty("Address")
        private String address;

        @JsonProperty("PinCode")
        private String pinCode;

        @JsonProperty("CityId")
        private String cityId;

        @JsonProperty("CountryName")
        private String countryName;

        @JsonProperty("PhoneNumber")
        private String phoneNumber;

        @JsonProperty("FaxNumber")
        private String faxNumber;

        @JsonProperty("Map")
        private String map;

        @JsonProperty("HotelRating")
        private Integer hotelRating;

        @JsonProperty("CityName")
        private String cityName;

        @JsonProperty("CountryCode")
        private String countryCode;

        @JsonProperty("CheckInTime")
        private String checkInTime;

        @JsonProperty("CheckOutTime")
        private String checkOutTime;

        /** Present when {@code IsRoomDetailRequired} is true; structure varies. */
        @JsonProperty("Rooms")
        private JsonNode rooms;
    }
}
