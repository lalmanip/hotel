package com.vivance.hotel.infrastructure.aggregator.tbo.dto;

import com.fasterxml.jackson.annotation.JsonProperty;
import lombok.AllArgsConstructor;
import lombok.Data;

@Data
@AllArgsConstructor
public class TboAffiliatePreBookRequest {
    @JsonProperty("BookingCode")
    private String bookingCode;

    @JsonProperty("PaymentMode")
    private String paymentMode;
}

