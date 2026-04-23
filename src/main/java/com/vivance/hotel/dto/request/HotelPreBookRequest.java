package com.vivance.hotel.dto.request;

import com.fasterxml.jackson.annotation.JsonProperty;
import jakarta.validation.constraints.NotBlank;
import lombok.Data;

@Data
public class HotelPreBookRequest {

    @NotBlank(message = "BookingCode is required")
    @JsonProperty("BookingCode")
    private String bookingCode;

    /**
     * TBO accepts values like "Limit".
     * If omitted, backend defaults to "Limit".
     */
    @JsonProperty("PaymentMode")
    private String paymentMode;

    @JsonProperty("TokenId")
    private String tokenId;

    /**
     * Optional routing hint. If omitted, backend auto-detects TBO from the request shape.
     */
    @JsonProperty("Aggregator")
    private String aggregator;
}

