package com.vivance.hotel.infrastructure.aggregator.tbo.dto;

import com.fasterxml.jackson.annotation.JsonProperty;
import lombok.Builder;
import lombok.Data;

/**
 * Request body for POST /hotelservice.svc/rest/GetHotelRoom
 * Returns real-time room availability and pricing with cancellation policies.
 */
@Data
@Builder
public class TboGetRoomRequest {

    @JsonProperty("ResultIndex")
    private String resultIndex;

    @JsonProperty("HotelCode")
    private String hotelCode;

    @JsonProperty("EndUserIp")
    private String endUserIp;

    @JsonProperty("TokenId")
    private String tokenId;

    @JsonProperty("TraceId")
    private String traceId;
}
