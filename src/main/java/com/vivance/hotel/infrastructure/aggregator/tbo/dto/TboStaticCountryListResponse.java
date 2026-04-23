package com.vivance.hotel.infrastructure.aggregator.tbo.dto;

import com.fasterxml.jackson.annotation.JsonProperty;
import lombok.Data;

import java.util.List;

@Data
public class TboStaticCountryListResponse {

    @JsonProperty("Status")
    private TboStaticStatus status;

    @JsonProperty("CountryList")
    private List<CountryItem> countryList;

    @Data
    public static class CountryItem {
        @JsonProperty("Code")
        private String code;

        @JsonProperty("Name")
        private String name;
    }
}

