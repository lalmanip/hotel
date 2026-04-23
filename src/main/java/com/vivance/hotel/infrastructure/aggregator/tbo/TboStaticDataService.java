package com.vivance.hotel.infrastructure.aggregator.tbo;

import com.vivance.hotel.config.AggregatorProperties;
import com.vivance.hotel.domain.entity.TboCity;
import com.vivance.hotel.domain.entity.TboCountry;
import com.vivance.hotel.domain.entity.TboHotelStatic;
import com.vivance.hotel.infrastructure.aggregator.tbo.dto.*;
import com.vivance.hotel.repository.TboCityRepository;
import com.vivance.hotel.repository.TboCountryRepository;
import com.vivance.hotel.repository.TboHotelStaticRepository;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.http.*;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.web.client.RestTemplate;

import java.util.Collections;
import java.util.List;

@Slf4j
@Service
@RequiredArgsConstructor
public class TboStaticDataService {

    private final RestTemplate tboRestTemplate;
    private final AggregatorProperties aggregatorProperties;
    private final TboCountryRepository countryRepository;
    private final TboCityRepository cityRepository;
    private final TboHotelStaticRepository hotelStaticRepository;

    @Transactional
    public List<TboCountry> syncCountries() {
        String url = aggregatorProperties.getTbo().getStaticBaseUrl() + "/CountryList";
        HttpEntity<Void> entity = new HttpEntity<>(basicAuthHeaders());

        ResponseEntity<TboStaticCountryListResponse> response = tboRestTemplate.exchange(
                url, HttpMethod.GET, entity, TboStaticCountryListResponse.class);

        TboStaticCountryListResponse body = response.getBody();
        List<TboStaticCountryListResponse.CountryItem> countries =
                body != null && body.getCountryList() != null ? body.getCountryList() : Collections.emptyList();

        List<TboCountry> rows = countries.stream()
                .map(item -> countryRepository.findByCode(item.getCode())
                        .map(existing -> {
                            existing.setName(item.getName());
                            return existing;
                        })
                        .orElseGet(() -> TboCountry.builder()
                                .code(item.getCode())
                                .name(item.getName())
                                .build()))
                .toList();

        List<TboCountry> saved = countryRepository.saveAll(rows);
        log.info("[TBO-STATIC] Synced {} countries", saved.size());
        return saved;
    }

    @Transactional
    public void syncCitiesForCountry(String countryCode) {
        String url = aggregatorProperties.getTbo().getStaticBaseUrl() + "/CityList";
        HttpEntity<TboStaticCityListRequest> entity =
                new HttpEntity<>(new TboStaticCityListRequest(countryCode), basicAuthHeaders());

        ResponseEntity<TboStaticCityListResponse> response = tboRestTemplate.exchange(
                url, HttpMethod.POST, entity, TboStaticCityListResponse.class);

        TboStaticCityListResponse body = response.getBody();
        List<TboStaticCityListResponse.CityItem> cities =
                body != null && body.getCityList() != null ? body.getCityList() : Collections.emptyList();

        List<TboCity> rows = cities.stream()
                .map(item -> cityRepository.findByCode(item.getCode())
                        .map(existing -> {
                            existing.setName(item.getName());
                            existing.setCountryCode(countryCode);
                            return existing;
                        })
                        .orElseGet(() -> TboCity.builder()
                                .code(item.getCode())
                                .name(item.getName())
                                .countryCode(countryCode)
                                .build()))
                .toList();

        cityRepository.saveAll(rows);
        log.info("[TBO-STATIC] Synced {} cities for country={}", rows.size(), countryCode);
    }

    @Transactional
    public void syncHotelsForCity(String cityCode) {
        String url = aggregatorProperties.getTbo().getStaticBaseUrl() + "/TBOHotelCodeList";
        HttpEntity<TboStaticHotelCodeListRequest> entity =
                new HttpEntity<>(new TboStaticHotelCodeListRequest(cityCode), basicAuthHeaders());

        ResponseEntity<TboStaticHotelCodeListResponse> response = tboRestTemplate.exchange(
                url, HttpMethod.POST, entity, TboStaticHotelCodeListResponse.class);

        TboStaticHotelCodeListResponse body = response.getBody();
        List<TboStaticHotelCodeListResponse.HotelItem> hotels =
                body != null && body.getHotels() != null ? body.getHotels() : Collections.emptyList();

        List<TboHotelStatic> rows = hotels.stream()
                .map(item -> hotelStaticRepository.findByHotelCode(item.getHotelCode())
                        .map(existing -> {
                            updateHotel(existing, item, cityCode);
                            return existing;
                        })
                        .orElseGet(() -> buildHotel(item, cityCode)))
                .toList();

        hotelStaticRepository.saveAll(rows);
        log.info("[TBO-STATIC] Synced {} hotels for city={}", rows.size(), cityCode);
    }

    private HttpHeaders basicAuthHeaders() {
        HttpHeaders headers = new HttpHeaders();
        headers.setBasicAuth(
                aggregatorProperties.getTbo().getStaticUserName(),
                aggregatorProperties.getTbo().getStaticPassword()
        );
        headers.setContentType(MediaType.APPLICATION_JSON);
        headers.setAccept(List.of(MediaType.APPLICATION_JSON));
        return headers;
    }

    private TboHotelStatic buildHotel(TboStaticHotelCodeListResponse.HotelItem h, String cityCode) {
        return TboHotelStatic.builder()
                .hotelCode(h.getHotelCode())
                .hotelName(h.getHotelName())
                .latitude(h.getLatitude())
                .longitude(h.getLongitude())
                .hotelRating(h.getHotelRating())
                .address(h.getAddress())
                .countryName(h.getCountryName())
                .countryCode(h.getCountryCode())
                .cityCode(cityCode)
                .cityName(h.getCityName())
                .build();
    }

    private void updateHotel(TboHotelStatic existing, TboStaticHotelCodeListResponse.HotelItem h, String cityCode) {
        existing.setHotelName(h.getHotelName());
        existing.setLatitude(h.getLatitude());
        existing.setLongitude(h.getLongitude());
        existing.setHotelRating(h.getHotelRating());
        existing.setAddress(h.getAddress());
        existing.setCountryName(h.getCountryName());
        existing.setCountryCode(h.getCountryCode());
        existing.setCityCode(cityCode);
        existing.setCityName(h.getCityName());
    }
}

