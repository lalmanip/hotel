package com.vivance.hotel.infrastructure.aggregator.tbo.dto;

import com.fasterxml.jackson.annotation.JsonProperty;
import lombok.Data;

@Data
public class TboStaticStatus {
    @JsonProperty("Code")
    private Integer code;

    @JsonProperty("Description")
    private String description;
}

