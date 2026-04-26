package com.vivance.hotel.service;

import com.vivance.hotel.dto.response.HotelCountryDto;
import com.vivance.hotel.repository.TboHotelCountryRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;

@Service
@RequiredArgsConstructor
public class HotelCountryService {

    private final TboHotelCountryRepository countryRepository;

    @Transactional(readOnly = true)
    public List<HotelCountryDto> listCountriesOrderedByName() {
        return countryRepository.findAllByOrderByNameAsc().stream()
                .map(c -> HotelCountryDto.builder()
                        .id(c.getId())
                        .code(c.getCode())
                        .name(c.getName())
                        .build())
                .toList();
    }
}
