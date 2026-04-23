package com.vivance.hotel.infrastructure.aggregator.tbo.dto;

import com.fasterxml.jackson.annotation.JsonInclude;
import com.fasterxml.jackson.annotation.JsonProperty;
import lombok.Builder;
import lombok.Data;

/**
 * Request body for POST {@code .../rest/Getbookingdetail} on HotelBE.
 * Provide either {@code BookingId} or {@code TraceId} (plus {@code TokenId} and {@code EndUserIp}).
 */
@Data
@Builder
@JsonInclude(JsonInclude.Include.NON_NULL)
public class TboGetBookingDetailRequest {

    /** Provide this to look up a specific booking by TBO's numeric booking ID. */
    @JsonProperty("BookingId")
    private String bookingId;

    @JsonProperty("EndUserIp")
    private String endUserIp;

    @JsonProperty("TokenId")
    private String tokenId;

    /** Provide this to retrieve the booking made during this search session. */
    @JsonProperty("TraceId")
    private String traceId;
}
