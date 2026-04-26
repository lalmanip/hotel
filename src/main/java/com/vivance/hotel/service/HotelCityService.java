package com.vivance.hotel.service;

import com.vivance.hotel.dto.response.HotelCityDto;
import com.vivance.hotel.repository.TboHotelCityRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.util.StringUtils;

import java.util.List;

@Service
@RequiredArgsConstructor
public class HotelCityService {

    private final TboHotelCityRepository cityRepository;

    @Transactional(readOnly = true)
    public List<HotelCityDto> listCitiesByCountryCode(String countryCode) {
        if (!StringUtils.hasText(countryCode)) {
            throw new IllegalArgumentException("countryCode is required");
        }
        return cityRepository.findByCountryCodeIgnoreCaseOrderByNameAsc(countryCode.trim()).stream()
                .map(c -> HotelCityDto.builder()
                        .id(c.getId())
                        .code(c.getCode())
                        .countryCode(c.getCountryCode())
                        .name(c.getName())
                        .build())
                .toList();
    }
}
