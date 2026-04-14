package com.vivance.hotel.infrastructure.aggregator.tbo.dto;

import com.fasterxml.jackson.annotation.JsonProperty;
import lombok.Builder;
import lombok.Data;

/**
 * Request body for POST /hotelservice.svc/rest/GetHotelInfo
 * ResultIndex and TraceId come from the search response.
 */
@Data
@Builder
public class TboHotelInfoRequest {

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
