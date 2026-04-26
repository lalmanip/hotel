package com.vivance.hotel.controller;

import com.vivance.hotel.dto.response.ApiResponse;
import com.vivance.hotel.dto.response.LocationSearchResultDto;
import com.vivance.hotel.service.HotelLocationSearchService;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.Parameter;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.validation.constraints.Max;
import jakarta.validation.constraints.Min;
import jakarta.validation.constraints.Size;
import lombok.RequiredArgsConstructor;
import org.springframework.http.ResponseEntity;
import org.springframework.validation.annotation.Validated;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

import java.util.List;

@Validated
@RestController
@RequiredArgsConstructor
@RequestMapping({"/api/hotels", "/api/v1/hotels"})
@Tag(name = "Hotel Locations", description = "Typeahead search for cities and hotels")
public class HotelLocationController {

    private final HotelLocationSearchService hotelLocationSearchService;

    @GetMapping("/locations")
    @Operation(
            summary = "Search hotel locations (cities + hotels)",
            description = "Returns a mixed list where cities are prioritized over hotels, sorted by relevance (prefix > contains), capped by limit."
    )
    public ResponseEntity<ApiResponse<List<LocationSearchResultDto>>> searchLocations(
            @RequestParam("query")
            @Size(min = 2, max = 100, message = "query must be between 2 and 100 characters")
            @Parameter(description = "Search text", example = "dubai", required = true)
            String query,

            @RequestParam(value = "limit", required = false, defaultValue = "15")
            @Min(value = 1, message = "limit must be >= 1")
            @Max(value = 50, message = "limit must be <= 50")
            @Parameter(description = "Max total results (cities first, then hotels)", example = "15")
            Integer limit
    ) {
        List<LocationSearchResultDto> results = hotelLocationSearchService.searchLocations(query, limit);
        return ResponseEntity.ok(ApiResponse.success(results));
    }
}

