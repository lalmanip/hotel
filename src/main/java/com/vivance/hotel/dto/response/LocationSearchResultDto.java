package com.vivance.hotel.dto.response;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

@Data
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class LocationSearchResultDto {

    /**
     * "city" or "hotel"
     */
    private String type;

    /**
     * City: TBO city code. Hotel: hotel id (or external code if available).
     */
    private String id;

    /**
     * Display name
     */
    private String name;

    /**
     * City: country code. Hotel: city/country.
     */
    private String secondaryText;
}

