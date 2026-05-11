package com.vivance.hotel.infrastructure.aggregator.tbo.dto;

import com.fasterxml.jackson.annotation.JsonProperty;
import jakarta.validation.constraints.NotBlank;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

/**
 * Request body for TBO static {@code Hoteldetails}.
 * {@code Hotelcodes} is a single code or comma-separated list (up to 50 codes per TBO guidance).
 */
@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class TboStaticHotelDetailsRequest {

    @JsonProperty("Hotelcodes")
    @NotBlank
    private String hotelcodes;

    @JsonProperty("Language")
    private String language;

    @JsonProperty("IsRoomDetailRequired")
    private Boolean isRoomDetailRequired;
}
