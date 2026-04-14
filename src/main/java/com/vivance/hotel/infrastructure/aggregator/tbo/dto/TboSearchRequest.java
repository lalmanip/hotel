package com.vivance.hotel.infrastructure.aggregator.tbo.dto;

import com.fasterxml.jackson.annotation.JsonProperty;
import lombok.Builder;
import lombok.Data;

import java.time.LocalDate;

/** Request DTO shaped to match TBO API contract. */
@Data
@Builder
public class TboSearchRequest {

    @JsonProperty("CityCode")
    private String cityCode;

    @JsonProperty("CheckIn")
    private LocalDate checkIn;

    @JsonProperty("CheckOut")
    private LocalDate checkOut;

    @JsonProperty("GuestCount")
    private int guestCount;

    @JsonProperty("RoomCount")
    @Builder.Default
    private int roomCount = 1;

    @JsonProperty("ResponseTime")
    @Builder.Default
    private int responseTime = 23;

    @JsonProperty("IsDetailedResponse")
    @Builder.Default
    private boolean isDetailedResponse = true;
}
