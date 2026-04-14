package com.vivance.hotel.infrastructure.aggregator.tbo.dto;

import com.fasterxml.jackson.annotation.JsonProperty;
import lombok.Data;

import java.util.List;

/** Response from POST /hotelservice.svc/rest/Gethotelresult */
@Data
public class TboHotelSearchResponse {

    @JsonProperty("HotelSearchResult")
    private HotelSearchResult hotelSearchResult;

    @Data
    public static class HotelSearchResult {

        /** 1 = success, 2 = failed */
        @JsonProperty("ResponseStatus")
        private int responseStatus;

        @JsonProperty("Error")
        private TboError error;

        /** Must be stored and sent in all subsequent API calls for this search session. */
        @JsonProperty("TraceId")
        private String traceId;

        @JsonProperty("CityId")
        private String cityId;

        @JsonProperty("CheckInDate")
        private String checkInDate;

        @JsonProperty("CheckOutDate")
        private String checkOutDate;

        @JsonProperty("PreferredCurrency")
        private String preferredCurrency;

        @JsonProperty("NoOfRooms")
        private int noOfRooms;

        @JsonProperty("HotelResults")
        private List<TboHotelResult> hotelResults;
    }

    @Data
    public static class TboHotelResult {

        @JsonProperty("IsHotDeal")
        private boolean isHotDeal;

        /** Index used in all subsequent API calls (GetHotelInfo, GetHotelRoom, Block, Book). */
        @JsonProperty("ResultIndex")
        private int resultIndex;

        @JsonProperty("HotelCode")
        private String hotelCode;

        @JsonProperty("HotelName")
        private String hotelName;

        @JsonProperty("HotelCategory")
        private String hotelCategory;

        @JsonProperty("StarRating")
        private int starRating;

        @JsonProperty("HotelDescription")
        private String hotelDescription;

        @JsonProperty("Price")
        private TboPrice price;

        @JsonProperty("HotelPicture")
        private String hotelPicture;

        @JsonProperty("HotelAddress")
        private String hotelAddress;

        @JsonProperty("HotelContactNo")
        private String hotelContactNo;

        @JsonProperty("Latitude")
        private String latitude;

        @JsonProperty("Longitude")
        private String longitude;
    }
}
