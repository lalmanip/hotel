package com.vivance.hotel.infrastructure.aggregator.tbo.dto;

import com.fasterxml.jackson.annotation.JsonProperty;
import lombok.Data;

import java.util.List;

@Data
public class TboStaticCityListResponse {

    @JsonProperty("Status")
    private TboStaticStatus status;

    @JsonProperty("CityList")
    private List<CityItem> cityList;

    @Data
    public static class CityItem {
        @JsonProperty("Code")
        private String code;

        @JsonProperty("Name")
        private String name;
    }
}

