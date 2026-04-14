package com.vivance.hotel.dto.response;

import lombok.Builder;
import lombok.Data;

import java.util.List;

@Data
@Builder
public class HotelDetailDto {

    private Long id;
    private String name;
    private String city;
    private String country;
    private String address;
    private String description;
    private Integer starRating;
    private List<String> amenities;
    private List<String> imageUrls;
    private String checkInTime;
    private String checkOutTime;
    private List<RoomAvailabilityDto> rooms;
}
