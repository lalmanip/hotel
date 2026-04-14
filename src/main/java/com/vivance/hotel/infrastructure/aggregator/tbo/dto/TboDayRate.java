package com.vivance.hotel.infrastructure.aggregator.tbo.dto;

import com.fasterxml.jackson.annotation.JsonProperty;
import lombok.Data;

import java.math.BigDecimal;
import java.time.LocalDateTime;

@Data
public class TboDayRate {

    @JsonProperty("Amount")
    private BigDecimal amount;

    @JsonProperty("Date")
    private LocalDateTime date;
}
