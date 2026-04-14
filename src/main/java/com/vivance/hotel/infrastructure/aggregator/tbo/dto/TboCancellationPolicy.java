package com.vivance.hotel.infrastructure.aggregator.tbo.dto;

import com.fasterxml.jackson.annotation.JsonProperty;
import lombok.Data;

import java.math.BigDecimal;
import java.time.LocalDateTime;

@Data
public class TboCancellationPolicy {

    /** Charge amount. Interpretation depends on ChargeType. */
    @JsonProperty("Charge")
    private BigDecimal charge;

    /**
     * ChargeType:
     * 1 = Fixed amount
     * 2 = Percentage of total
     * 3 = Number of nights
     */
    @JsonProperty("ChargeType")
    private int chargeType;

    @JsonProperty("Currency")
    private String currency;

    @JsonProperty("FromDate")
    private LocalDateTime fromDate;

    @JsonProperty("ToDate")
    private LocalDateTime toDate;

    @JsonProperty("NoShowPolicy")
    private Boolean noShowPolicy;
}
