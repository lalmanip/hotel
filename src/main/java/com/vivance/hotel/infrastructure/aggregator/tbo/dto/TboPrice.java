package com.vivance.hotel.infrastructure.aggregator.tbo.dto;

import com.fasterxml.jackson.annotation.JsonProperty;
import lombok.Data;

import java.math.BigDecimal;

/**
 * TBO price object — appears in search, room, block, and book responses.
 * Use {@code OfferedPrice} as the price displayed to the customer.
 */
@Data
public class TboPrice {

    @JsonProperty("CurrencyCode")              private String currencyCode;
    @JsonProperty("RoomPrice")                 private BigDecimal roomPrice;
    @JsonProperty("Tax")                       private BigDecimal tax;
    @JsonProperty("ExtraGuestCharge")          private BigDecimal extraGuestCharge;
    @JsonProperty("ChildCharge")               private BigDecimal childCharge;
    @JsonProperty("OtherCharges")              private BigDecimal otherCharges;
    @JsonProperty("Discount")                  private BigDecimal discount;
    @JsonProperty("PublishedPrice")            private BigDecimal publishedPrice;
    @JsonProperty("PublishedPriceRoundedOff")  private BigDecimal publishedPriceRoundedOff;
    @JsonProperty("OfferedPrice")              private BigDecimal offeredPrice;
    @JsonProperty("OfferedPriceRoundedOff")    private BigDecimal offeredPriceRoundedOff;
    @JsonProperty("AgentCommission")           private BigDecimal agentCommission;
    @JsonProperty("AgentMarkUp")               private BigDecimal agentMarkUp;
    @JsonProperty("ServiceTax")                private BigDecimal serviceTax;
    @JsonProperty("TCS")                       private BigDecimal tcs;
    @JsonProperty("TDS")                       private BigDecimal tds;
    @JsonProperty("ServiceCharge")             private BigDecimal serviceCharge;
    @JsonProperty("TotalGSTAmount")            private BigDecimal totalGstAmount;
    @JsonProperty("GST")                       private TboGst gst;
}
