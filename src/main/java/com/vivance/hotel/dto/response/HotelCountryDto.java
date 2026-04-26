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
@Schema(description = "TBO hotel country row from tbo_hotel_countries")
public class HotelCountryDto {

    @Schema(description = "Primary key")
    private Long id;

    @Schema(description = "ISO / TBO country code", example = "IN")
    private String code;

    @Schema(description = "Display name", example = "India")
    private String name;
}
