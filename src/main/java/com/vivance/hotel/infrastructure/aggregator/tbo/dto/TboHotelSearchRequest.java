package com.vivance.hotel.infrastructure.aggregator.tbo.dto;

import com.fasterxml.jackson.annotation.JsonProperty;
import lombok.Builder;
import lombok.Data;

import java.util.List;

/**
 * Request body for POST /hotelservice.svc/rest/Gethotelresult
 * CheckInDate must be formatted as dd/MM/yyyy.
 */
@Data
@Builder
public class TboHotelSearchRequest {

    /** dd/MM/yyyy format required by TBO */
    @JsonProperty("CheckInDate")
    private String checkInDate;

    @JsonProperty("NoOfNights")
    private String noOfNights;

    @JsonProperty("CountryCode")
    private String countryCode;

    /** TBO's numeric city identifier (e.g. "130443" for Delhi) */
    @JsonProperty("CityId")
    private String cityId;

    @JsonProperty("ResultCount")
    private Integer resultCount;

    @JsonProperty("PreferredCurrency")
    @Builder.Default
    private String preferredCurrency = "INR";

    @JsonProperty("GuestNationality")
    @Builder.Default
    private String guestNationality = "IN";

    @JsonProperty("NoOfRooms")
    private String noOfRooms;

    @JsonProperty("RoomGuests")
    private List<TboRoomGuest> roomGuests;

    @JsonProperty("MaxRating")
    @Builder.Default
    private int maxRating = 5;

    @JsonProperty("MinRating")
    @Builder.Default
    private int minRating = 0;

    @JsonProperty("ReviewScore")
    private String reviewScore;

    @JsonProperty("IsNearBySearchAllowed")
    @Builder.Default
    private boolean isNearBySearchAllowed = false;

    @JsonProperty("EndUserIp")
    private String endUserIp;

    @JsonProperty("TokenId")
    private String tokenId;
}
