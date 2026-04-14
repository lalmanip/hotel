package com.vivance.hotel.mapper;

import com.vivance.hotel.domain.entity.Booking;
import com.vivance.hotel.dto.response.BookingDto;
import org.mapstruct.Mapper;
import org.mapstruct.Mapping;

import java.util.List;

@Mapper(componentModel = "spring")
public interface BookingMapper {

    @Mapping(target = "hotelId", source = "hotel.id")
    @Mapping(target = "hotelName", source = "hotel.name")
    @Mapping(target = "roomId", source = "room.id")
    @Mapping(target = "roomType", source = "room.roomType")
    BookingDto toBookingDto(Booking booking);

    List<BookingDto> toBookingDtoList(List<Booking> bookings);
}
