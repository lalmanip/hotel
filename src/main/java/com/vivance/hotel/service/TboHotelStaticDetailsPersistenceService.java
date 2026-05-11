package com.vivance.hotel.service;

import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.vivance.hotel.domain.entity.TboHotelStaticDetails;
import com.vivance.hotel.infrastructure.aggregator.tbo.dto.TboStaticHotelDetailsResponse;
import com.vivance.hotel.repository.TboHotelStaticDetailsRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.util.StringUtils;

import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import java.util.function.Function;
import java.util.stream.Collectors;

@Service
@RequiredArgsConstructor
public class TboHotelStaticDetailsPersistenceService {

    private final TboHotelStaticDetailsRepository detailsRepository;
    private final ObjectMapper objectMapper;

    @Transactional
    public int upsertFromResponse(TboStaticHotelDetailsResponse resp) {
        if (resp == null || resp.getHotelDetails() == null || resp.getHotelDetails().isEmpty()) {
            return 0;
        }

        List<String> codes = resp.getHotelDetails().stream()
                .map(TboStaticHotelDetailsResponse.HotelDetailItem::getHotelCode)
                .filter(StringUtils::hasText)
                .toList();
        if (codes.isEmpty()) {
            return 0;
        }

        Map<String, TboHotelStaticDetails> existing = detailsRepository.findAllByHotelCodeIn(codes).stream()
                .collect(Collectors.toMap(TboHotelStaticDetails::getHotelCode, Function.identity(), (a, b) -> a));

        LocalDateTime fetchedAt = LocalDateTime.now();
        List<TboHotelStaticDetails> toSave = new ArrayList<>();

        for (TboStaticHotelDetailsResponse.HotelDetailItem item : resp.getHotelDetails()) {
            if (!StringUtils.hasText(item.getHotelCode())) {
                continue;
            }
            TboHotelStaticDetails row = existing.get(item.getHotelCode());
            if (row == null) {
                row = TboHotelStaticDetails.builder()
                        .hotelCode(item.getHotelCode())
                        .build();
            }
            mapItemToRow(row, item);
            row.setFetchedAt(fetchedAt);
            toSave.add(row);
        }

        detailsRepository.saveAll(toSave);
        return toSave.size();
    }

    private void mapItemToRow(TboHotelStaticDetails row, TboStaticHotelDetailsResponse.HotelDetailItem item) {
        row.setHotelName(trimToNull(item.getHotelName()));
        row.setDescription(trimToNull(item.getDescription()));
        row.setHotelFacilitiesJson(writeJson(item.getHotelFacilities()));
        row.setAttractionsJson(writeJsonNode(item.getAttractions()));
        row.setImageUrl(trimToNull(item.getImage()));
        row.setImagesJson(writeJson(item.getImages()));
        row.setAddress(trimToNull(item.getAddress()));
        row.setPinCode(trimToNull(item.getPinCode()));
        row.setCityId(trimToNull(item.getCityId()));
        row.setCityName(trimToNull(item.getCityName()));
        row.setCountryName(trimToNull(item.getCountryName()));
        row.setCountryCode(trimToNull(item.getCountryCode()));
        row.setPhoneNumber(trimToNull(item.getPhoneNumber()));
        row.setEmail(trimToNull(item.getEmail()));
        row.setHotelWebsiteUrl(trimToNull(item.getHotelWebsiteUrl()));
        row.setFaxNumber(trimToNull(item.getFaxNumber()));
        row.setMapCoordinates(trimToNull(item.getMap()));
        row.setHotelRating(item.getHotelRating());
        row.setCheckInTime(trimToNull(item.getCheckInTime()));
        row.setCheckOutTime(trimToNull(item.getCheckOutTime()));
        row.setHotelFeesJson(writeJsonNode(item.getHotelFees()));
    }

    private String writeJson(Object value) {
        if (value == null) {
            return null;
        }
        try {
            return objectMapper.writeValueAsString(value);
        } catch (JsonProcessingException e) {
            throw new IllegalStateException("JSON serialize failed", e);
        }
    }

    private String writeJsonNode(com.fasterxml.jackson.databind.JsonNode node) {
        if (node == null || node.isNull()) {
            return null;
        }
        if (node.isObject() && node.isEmpty()) {
            return "{}";
        }
        if (node.isArray() && node.isEmpty()) {
            return "[]";
        }
        try {
            return objectMapper.writeValueAsString(node);
        } catch (JsonProcessingException e) {
            throw new IllegalStateException("JSON serialize failed", e);
        }
    }

    private static String trimToNull(String s) {
        if (!StringUtils.hasText(s)) {
            return null;
        }
        String t = s.trim();
        return t.isEmpty() ? null : t;
    }
}
