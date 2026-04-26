package com.vivance.hotel.dto.response;

import io.swagger.v3.oas.annotations.media.Schema;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
@Schema(description = "TBO hotel city row from tbo_hotel_cities")
public class HotelCityDto {

    @Schema(description = "Primary key")
    private Long id;

    @Schema(description = "TBO city code (use as cityId in search)", example = "130443")
    private String code;

    @Schema(description = "ISO / TBO country code", example = "IN")
    private String countryCode;

    @Schema(description = "Display name", example = "Mumbai")
    private String name;
}
