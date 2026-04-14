package com.vivance.hotel.infrastructure.aggregator.tbo.dto;

import com.fasterxml.jackson.annotation.JsonProperty;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.util.List;

/** Per-room guest breakdown sent in the hotel search request. */
@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class TboRoomGuest {

    @JsonProperty("NoOfAdults")
    private int noOfAdults;

    @JsonProperty("NoOfChild")
    @Builder.Default
    private int noOfChild = 0;

    @JsonProperty("ChildAge")
    private List<Integer> childAge;
}
