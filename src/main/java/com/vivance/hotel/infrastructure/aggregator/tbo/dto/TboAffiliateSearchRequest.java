package com.vivance.hotel.infrastructure.aggregator.tbo.dto;

import com.fasterxml.jackson.annotation.JsonProperty;
import lombok.Builder;
import lombok.Data;

import java.util.List;

@Data
@Builder
public class TboAffiliateSearchRequest {

    @JsonProperty("CheckIn")
    private String checkIn;

    @JsonProperty("CheckOut")
    private String checkOut;

    @JsonProperty("HotelCodes")
    private String hotelCodes;

    @JsonProperty("GuestNationality")
    private String guestNationality;

    @JsonProperty("PaxRooms")
    private List<PaxRoom> paxRooms;

    @JsonProperty("ResponseTime")
    @Builder.Default
    private Double responseTime = 23.0;

    @JsonProperty("IsDetailedResponse")
    @Builder.Default
    private Boolean isDetailedResponse = true;

    @JsonProperty("TokenId")
    private String tokenId;

    @JsonProperty("EndUserIp")
    private String endUserIp;

    @JsonProperty("Filters")
    private Filters filters;

    @Data
    @Builder
    public static class PaxRoom {
        @JsonProperty("Adults")
        private Integer adults;

        @JsonProperty("Children")
        private Integer children;

        @JsonProperty("ChildrenAges")
        private List<Integer> childrenAges;
    }

    @Data
    @Builder
    public static class Filters {
        @JsonProperty("Refundable")
        private Boolean refundable;

        @JsonProperty("NoOfRooms")
        private Integer noOfRooms;

        @JsonProperty("MealType")
        private String mealType;

        @JsonProperty("StarRating")
        private Integer starRating;
    }
}

