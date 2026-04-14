package com.vivance.hotel.infrastructure.aggregator.tbo.dto;

import com.fasterxml.jackson.annotation.JsonProperty;
import lombok.Data;

import java.util.List;

/** Response from POST /hotelservice.svc/rest/GetHotelInfo */
@Data
public class TboHotelInfoResponse {

    @JsonProperty("HotelInfoResult")
    private HotelInfoResult hotelInfoResult;

    @Data
    public static class HotelInfoResult {

        @JsonProperty("ResponseStatus")
        private int responseStatus;

        @JsonProperty("Error")
        private TboError error;

        @JsonProperty("TraceId")
        private String traceId;

        @JsonProperty("HotelDetails")
        private HotelDetails hotelDetails;
    }

    @Data
    public static class HotelDetails {

        @JsonProperty("HotelCode")
        private String hotelCode;

        @JsonProperty("HotelName")
        private String hotelName;

        @JsonProperty("StarRating")
        private int starRating;

        @JsonProperty("HotelURL")
        private String hotelUrl;

        @JsonProperty("Description")
        private String description;

        @JsonProperty("Attractions")
        private List<TboAttraction> attractions;

        @JsonProperty("HotelFacilities")
        private List<String> hotelFacilities;

        @JsonProperty("HotelPolicy")
        private String hotelPolicy;

        @JsonProperty("SpecialInstructions")
        private String specialInstructions;

        @JsonProperty("HotelPicture")
        private String hotelPicture;

        @JsonProperty("Images")
        private List<String> images;

        @JsonProperty("Address")
        private String address;

        @JsonProperty("CountryName")
        private String countryName;

        @JsonProperty("PinCode")
        private String pinCode;

        @JsonProperty("HotelContactNo")
        private String hotelContactNo;

        @JsonProperty("FaxNumber")
        private String faxNumber;

        @JsonProperty("Email")
        private String email;

        @JsonProperty("Latitude")
        private String latitude;

        @JsonProperty("Longitude")
        private String longitude;
    }

    @Data
    public static class TboAttraction {
        @JsonProperty("Key")
        private String key;
        @JsonProperty("Value")
        private String value;
    }
}
