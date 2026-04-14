package com.vivance.hotel.mapper;

import com.vivance.hotel.domain.entity.Hotel;
import com.vivance.hotel.domain.entity.Room;
import com.vivance.hotel.dto.response.HotelDetailDto;
import com.vivance.hotel.dto.response.HotelDto;
import com.vivance.hotel.dto.response.RoomAvailabilityDto;
import org.mapstruct.Mapper;
import org.mapstruct.Mapping;
import org.mapstruct.Named;

import java.util.Arrays;
import java.util.Collections;
import java.util.List;

/**
 * MapStruct mapper for Hotel and Room entities to DTOs.
 * Spring component model ensures injection via {@code @Autowired}.
 */
@Mapper(componentModel = "spring")
public interface HotelMapper {

    @Mapping(target = "amenities", source = "amenities", qualifiedByName = "splitCsv")
    @Mapping(target = "imageUrls", source = "imageUrls", qualifiedByName = "splitCsv")
    @Mapping(target = "startingPrice", ignore = true)
    @Mapping(target = "currency", ignore = true)
    @Mapping(target = "aggregatorSource", ignore = true)
    @Mapping(target = "externalHotelId", ignore = true)
    HotelDto toHotelDto(Hotel hotel);

    @Mapping(target = "amenities", source = "amenities", qualifiedByName = "splitCsv")
    @Mapping(target = "imageUrls", source = "imageUrls", qualifiedByName = "splitCsv")
    @Mapping(target = "rooms", ignore = true)
    HotelDetailDto toHotelDetailDto(Hotel hotel);

    @Mapping(target = "amenities", source = "amenities", qualifiedByName = "splitCsv")
    @Mapping(target = "imageUrls", source = "imageUrls", qualifiedByName = "splitCsv")
    @Mapping(target = "totalPrice", ignore = true)
    RoomAvailabilityDto toRoomAvailabilityDto(Room room);

    List<HotelDto> toHotelDtoList(List<Hotel> hotels);

    List<RoomAvailabilityDto> toRoomAvailabilityDtoList(List<Room> rooms);

    @Named("splitCsv")
    default List<String> splitCsv(String csv) {
        if (csv == null || csv.isBlank()) return Collections.emptyList();
        return Arrays.stream(csv.split(","))
                .map(String::trim)
                .filter(s -> !s.isEmpty())
                .toList();
    }
}
