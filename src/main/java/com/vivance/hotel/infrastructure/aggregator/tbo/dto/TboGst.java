package com.vivance.hotel.infrastructure.aggregator.tbo.dto;

import com.fasterxml.jackson.annotation.JsonProperty;
import lombok.Data;

import java.math.BigDecimal;

@Data
public class TboGst {

    @JsonProperty("CGSTAmount")   private BigDecimal cgstAmount;
    @JsonProperty("CGSTRate")     private BigDecimal cgstRate;
    @JsonProperty("CessAmount")   private BigDecimal cessAmount;
    @JsonProperty("CessRate")     private BigDecimal cessRate;
    @JsonProperty("IGSTAmount")   private BigDecimal igstAmount;
    @JsonProperty("IGSTRate")     private BigDecimal igstRate;
    @JsonProperty("SGSTAmount")   private BigDecimal sgstAmount;
    @JsonProperty("SGSTRate")     private BigDecimal sgstRate;
    @JsonProperty("TaxableAmount")private BigDecimal taxableAmount;
}
