package com.vivance.hotel.controller;

import com.vivance.hotel.dto.response.ApiResponse;
import com.vivance.hotel.infrastructure.aggregator.tbo.TboStaticDataService;
import com.vivance.hotel.repository.TboCityRepository;
import com.vivance.hotel.repository.TboCountryRepository;
import com.vivance.hotel.repository.TboHotelStaticRepository;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.util.HashMap;
import java.util.Map;

@Slf4j
@RestController
@RequestMapping("/api/v1/admin/tbo/static")
@RequiredArgsConstructor
@Tag(name = "TBO Static Admin", description = "Admin operations for TBO static data (countries, cities, hotels)")
public class TboStaticAdminController {

    private final TboStaticDataService staticDataService;
    private final TboCountryRepository countryRepository;
    private final TboCityRepository cityRepository;
    private final TboHotelStaticRepository hotelStaticRepository;

    @PostMapping("/refresh")
    @Operation(summary = "Trigger full TBO static data refresh",
               description = "Fetches countries, then cities per country, then hotels per city and stores them in DB.")
    public ResponseEntity<ApiResponse<Void>> refreshAll() throws InterruptedException {
        log.info("[TBO-STATIC] Manual trigger: full static data refresh");

        var countries = staticDataService.syncCountries();
        countries.forEach(country -> {
            try {
                staticDataService.syncCitiesForCountry(country.getCode());
                Thread.sleep(200L);
            } catch (InterruptedException e) {
                Thread.currentThread().interrupt();
                throw new RuntimeException("Interrupted while syncing cities for " + country.getCode(), e);
            }
        });

        var cities = cityRepository.findAll();
        for (var city : cities) {
            staticDataService.syncHotelsForCity(city.getCode());
            Thread.sleep(200L);
        }

        return ResponseEntity.ok(ApiResponse.success("TBO static data refresh triggered successfully", null));
    }

    @GetMapping("/stats")
    @Operation(summary = "Get basic statistics for TBO static data",
               description = "Returns counts of countries, cities, and hotels currently stored.")
    public ResponseEntity<ApiResponse<Map<String, Long>>> getStats() {
        long countryCount = countryRepository.count();
        long cityCount = cityRepository.count();
        long hotelCount = hotelStaticRepository.count();

        Map<String, Long> stats = new HashMap<>();
        stats.put("countries", countryCount);
        stats.put("cities", cityCount);
        stats.put("hotels", hotelCount);

        return ResponseEntity.ok(ApiResponse.success("TBO static data stats", stats));
    }
}

