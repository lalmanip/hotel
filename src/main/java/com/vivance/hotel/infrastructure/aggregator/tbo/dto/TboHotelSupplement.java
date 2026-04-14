package com.vivance.hotel.infrastructure.aggregator.tbo.dto;

import com.fasterxml.jackson.annotation.JsonProperty;
import lombok.Data;

import java.math.BigDecimal;

@Data
public class TboHotelSupplement {

    @JsonProperty("SupplementId")
    private String supplementId;

    @JsonProperty("Name")
    private String name;

    /**
     * SupplementChargeType:
     * 1 = Per person per night
     * 2 = Per room per night
     * 3 = Per stay (mandatory fees like tourism tax)
     */
    @JsonProperty("SupplementChargeType")
    private int supplementChargeType;

    @JsonProperty("Price")
    private BigDecimal price;

    @JsonProperty("Currency")
    private String currency;
}
