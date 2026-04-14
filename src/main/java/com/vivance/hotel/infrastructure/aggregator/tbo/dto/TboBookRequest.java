package com.vivance.hotel.infrastructure.aggregator.tbo.dto;

import com.fasterxml.jackson.annotation.JsonProperty;
import lombok.Builder;
import lombok.Data;

import java.util.List;

/**
 * Request body for POST /hotelservice.svc/rest/book
 * Identical structure to Block, but HotelRoomsDetails must include HotelPassenger.
 */
@Data
@Builder
public class TboBookRequest {

    @JsonProperty("ResultIndex")
    private String resultIndex;

    @JsonProperty("HotelCode")
    private String hotelCode;

    @JsonProperty("HotelName")
    private String hotelName;

    @JsonProperty("GuestNationality")
    @Builder.Default
    private String guestNationality = "IN";

    @JsonProperty("NoOfRooms")
    private String noOfRooms;

    @JsonProperty("ClientReferenceNo")
    @Builder.Default
    private String clientReferenceNo = "0";

    @JsonProperty("IsVoucherBooking")
    @Builder.Default
    private String isVoucherBooking = "false";

    /** Must include HotelPassenger[] populated with actual guest details. */
    @JsonProperty("HotelRoomsDetails")
    private List<TboRoomDetail> hotelRoomsDetails;

    @JsonProperty("EndUserIp")
    private String endUserIp;

    @JsonProperty("TokenId")
    private String tokenId;

    @JsonProperty("TraceId")
    private String traceId;
}
