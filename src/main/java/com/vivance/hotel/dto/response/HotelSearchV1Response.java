package com.vivance.hotel.dto.response;

import lombok.Builder;
import lombok.Data;

import java.util.List;

@Data
@Builder
public class HotelSearchV1Response {

    private List<HotelSearchResultDto> hotels;
    private Pagination pagination;

    @Data
    @Builder
    public static class Pagination {
        private long total;
        private int page;
        private int size;
    }

    @Data
    @Builder
    public static class HotelSearchResultDto {
        private String hotelCode;
        private String name;
        private Double price;
        private Double rating;
        private Location location;
        private List<String> amenities;
    }

    @Data
    @Builder
    public static class Location {
        private Double lat;
        private Double lng;
    }
}

