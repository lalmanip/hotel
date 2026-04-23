package com.vivance.hotel.infrastructure.aggregator.tbo.dto;

import com.fasterxml.jackson.annotation.JsonProperty;
import lombok.Data;

@Data
public class TboAffiliateStatus {
    @JsonProperty("Code")
    private Integer code;

    @JsonProperty("Description")
    private String description;
}

