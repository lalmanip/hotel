package com.vivance.hotel.service;

import com.vivance.hotel.dto.response.LocationSearchResultDto;
import com.vivance.hotel.repository.HotelRepository;
import com.vivance.hotel.repository.TboHotelCityRepository;
import com.vivance.hotel.repository.TboHotelStaticRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;

import java.util.ArrayList;
import java.util.HashSet;
import java.util.List;
import java.util.Set;

@Service
@RequiredArgsConstructor
public class HotelLocationSearchService {

    private static final int DEFAULT_LIMIT = 15;
    private static final int MAX_LIMIT = 50;

    private final TboHotelCityRepository tboHotelCityRepository;
    private final HotelRepository hotelRepository;
    private final TboHotelStaticRepository tboHotelStaticRepository;

    public List<LocationSearchResultDto> searchLocations(String query, Integer limit) {
        String q = query == null ? "" : query.trim();
        if (q.length() < 2) {
            throw new IllegalArgumentException("query must be at least 2 characters");
        }

        int finalLimit = limit == null ? DEFAULT_LIMIT : limit;
        if (finalLimit < 1) {
            throw new IllegalArgumentException("limit must be >= 1");
        }
        if (finalLimit > MAX_LIMIT) {
            finalLimit = MAX_LIMIT;
        }

        // NOTE: For index-friendly prefix search, we avoid LOWER() in SQL and rely on a case-insensitive collation.
        // If your DB uses a case-sensitive collation, consider a computed lower(name) column + index.

        List<LocationSearchResultDto> out = new ArrayList<>(finalLimit);

        // Avoid duplicates across prefix/contains phases.
        Set<String> cityIds = new HashSet<>();
        Set<Long> hotelIds = new HashSet<>();

        // 1) Cities first (prefix then contains)
        addCities(q, finalLimit, out, cityIds);

        // 2) Hotels second (prefix then contains), only if we still have space
        if (out.size() < finalLimit) {
            // Prefer TBO static hotels (available in vivance_java) over local 'hotels' table.
            addStaticHotels(q, finalLimit, out, hotelIds);
        }

        return out;
    }

    private void addCities(String q, int finalLimit, List<LocationSearchResultDto> out, Set<String> cityIds) {
        int remaining = finalLimit - out.size();
        if (remaining <= 0) return;

        List<TboHotelCityRepository.CityLocationRow> prefix = tboHotelCityRepository.searchByNamePrefix(q, remaining);
        for (TboHotelCityRepository.CityLocationRow c : prefix) {
            if (out.size() >= finalLimit) break;
            if (c == null || c.getCode() == null) continue;
            if (!cityIds.add(c.getCode())) continue;
            out.add(LocationSearchResultDto.builder()
                    .type("city")
                    .id(c.getCode())
                    .name(c.getName())
                    .secondaryText(c.getCountryCode())
                    .build());
        }

        remaining = finalLimit - out.size();
        if (remaining <= 0) return;

        List<TboHotelCityRepository.CityLocationRow> contains =
                tboHotelCityRepository.searchByNameContainsExcludingPrefix(q, remaining);
        for (TboHotelCityRepository.CityLocationRow c : contains) {
            if (out.size() >= finalLimit) break;
            if (c == null || c.getCode() == null) continue;
            if (!cityIds.add(c.getCode())) continue;
            out.add(LocationSearchResultDto.builder()
                    .type("city")
                    .id(c.getCode())
                    .name(c.getName())
                    .secondaryText(c.getCountryCode())
                    .build());
        }
    }

    private void addHotels(String q, int finalLimit, List<LocationSearchResultDto> out, Set<Long> hotelIds) {
        int remaining = finalLimit - out.size();
        if (remaining <= 0) return;

        List<HotelRepository.HotelLocationRow> prefix = hotelRepository.searchByNamePrefix(q, remaining);
        for (HotelRepository.HotelLocationRow h : prefix) {
            if (out.size() >= finalLimit) break;
            if (h == null || h.getId() == null) continue;
            if (!hotelIds.add(h.getId())) continue;
            out.add(LocationSearchResultDto.builder()
                    .type("hotel")
                    .id(String.valueOf(h.getId()))
                    .name(h.getName())
                    .secondaryText(formatHotelSecondaryText(h.getCity(), h.getCountry()))
                    .build());
        }

        remaining = finalLimit - out.size();
        if (remaining <= 0) return;

        List<HotelRepository.HotelLocationRow> contains = hotelRepository.searchByNameContainsExcludingPrefix(q, remaining);
        for (HotelRepository.HotelLocationRow h : contains) {
            if (out.size() >= finalLimit) break;
            if (h == null || h.getId() == null) continue;
            if (!hotelIds.add(h.getId())) continue;
            out.add(LocationSearchResultDto.builder()
                    .type("hotel")
                    .id(String.valueOf(h.getId()))
                    .name(h.getName())
                    .secondaryText(formatHotelSecondaryText(h.getCity(), h.getCountry()))
                    .build());
        }
    }

    private void addStaticHotels(String q, int finalLimit, List<LocationSearchResultDto> out, Set<Long> hotelIds) {
        int remaining = finalLimit - out.size();
        if (remaining <= 0) return;

        // NOTE: we use String hash as a stable-ish de-dupe key here because static hotels don't have Long IDs in the API.
        List<TboHotelStaticRepository.StaticHotelLocationRow> prefix =
                tboHotelStaticRepository.searchStaticHotelsByNamePrefix(q, remaining);
        for (TboHotelStaticRepository.StaticHotelLocationRow h : prefix) {
            if (out.size() >= finalLimit) break;
            if (h == null || h.getHotelCode() == null) continue;
            long key = h.getHotelCode().hashCode();
            if (!hotelIds.add(key)) continue;
            out.add(LocationSearchResultDto.builder()
                    .type("hotel")
                    .id(h.getHotelCode())
                    .name(h.getHotelName() != null ? h.getHotelName() : h.getHotelCode())
                    .secondaryText(formatHotelSecondaryText(h.getCityName(), h.getCountryName()))
                    .build());
        }

        remaining = finalLimit - out.size();
        if (remaining <= 0) return;

        List<TboHotelStaticRepository.StaticHotelLocationRow> contains =
                tboHotelStaticRepository.searchStaticHotelsByNameContainsExcludingPrefix(q, remaining);
        for (TboHotelStaticRepository.StaticHotelLocationRow h : contains) {
            if (out.size() >= finalLimit) break;
            if (h == null || h.getHotelCode() == null) continue;
            long key = h.getHotelCode().hashCode();
            if (!hotelIds.add(key)) continue;
            out.add(LocationSearchResultDto.builder()
                    .type("hotel")
                    .id(h.getHotelCode())
                    .name(h.getHotelName() != null ? h.getHotelName() : h.getHotelCode())
                    .secondaryText(formatHotelSecondaryText(h.getCityName(), h.getCountryName()))
                    .build());
        }
    }

    private String formatHotelSecondaryText(String city, String country) {
        String c1 = city == null ? "" : city.trim();
        String c2 = country == null ? "" : country.trim();
        if (!c1.isEmpty() && !c2.isEmpty()) return c1 + ", " + c2;
        if (!c1.isEmpty()) return c1;
        return c2;
    }
}

