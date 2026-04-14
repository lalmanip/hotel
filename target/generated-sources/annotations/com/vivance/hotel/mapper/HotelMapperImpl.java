package com.vivance.hotel.mapper;

import com.vivance.hotel.domain.entity.Hotel;
import com.vivance.hotel.domain.entity.Room;
import com.vivance.hotel.dto.response.HotelDetailDto;
import com.vivance.hotel.dto.response.HotelDto;
import com.vivance.hotel.dto.response.RoomAvailabilityDto;
import java.util.ArrayList;
import java.util.List;
import javax.annotation.processing.Generated;
import org.springframework.stereotype.Component;

@Generated(
    value = "org.mapstruct.ap.MappingProcessor",
    date = "2026-04-13T20:57:45-0400",
    comments = "version: 1.5.5.Final, compiler: javac, environment: Java 21.0.10 (Oracle Corporation)"
)
@Component
public class HotelMapperImpl implements HotelMapper {

    @Override
    public HotelDto toHotelDto(Hotel hotel) {
        if ( hotel == null ) {
            return null;
        }

        HotelDto.HotelDtoBuilder hotelDto = HotelDto.builder();

        hotelDto.amenities( splitCsv( hotel.getAmenities() ) );
        hotelDto.imageUrls( splitCsv( hotel.getImageUrls() ) );
        hotelDto.id( hotel.getId() );
        hotelDto.name( hotel.getName() );
        hotelDto.city( hotel.getCity() );
        hotelDto.country( hotel.getCountry() );
        hotelDto.address( hotel.getAddress() );
        hotelDto.starRating( hotel.getStarRating() );

        return hotelDto.build();
    }

    @Override
    public HotelDetailDto toHotelDetailDto(Hotel hotel) {
        if ( hotel == null ) {
            return null;
        }

        HotelDetailDto.HotelDetailDtoBuilder hotelDetailDto = HotelDetailDto.builder();

        hotelDetailDto.amenities( splitCsv( hotel.getAmenities() ) );
        hotelDetailDto.imageUrls( splitCsv( hotel.getImageUrls() ) );
        hotelDetailDto.id( hotel.getId() );
        hotelDetailDto.name( hotel.getName() );
        hotelDetailDto.city( hotel.getCity() );
        hotelDetailDto.country( hotel.getCountry() );
        hotelDetailDto.address( hotel.getAddress() );
        hotelDetailDto.description( hotel.getDescription() );
        hotelDetailDto.starRating( hotel.getStarRating() );
        hotelDetailDto.checkInTime( hotel.getCheckInTime() );
        hotelDetailDto.checkOutTime( hotel.getCheckOutTime() );

        return hotelDetailDto.build();
    }

    @Override
    public RoomAvailabilityDto toRoomAvailabilityDto(Room room) {
        if ( room == null ) {
            return null;
        }

        RoomAvailabilityDto.RoomAvailabilityDtoBuilder roomAvailabilityDto = RoomAvailabilityDto.builder();

        roomAvailabilityDto.amenities( splitCsv( room.getAmenities() ) );
        roomAvailabilityDto.imageUrls( splitCsv( room.getImageUrls() ) );
        roomAvailabilityDto.id( room.getId() );
        roomAvailabilityDto.roomType( room.getRoomType() );
        roomAvailabilityDto.maxOccupancy( room.getMaxOccupancy() );
        roomAvailabilityDto.pricePerNight( room.getPricePerNight() );
        roomAvailabilityDto.currency( room.getCurrency() );
        roomAvailabilityDto.available( room.isAvailable() );
        roomAvailabilityDto.description( room.getDescription() );

        return roomAvailabilityDto.build();
    }

    @Override
    public List<HotelDto> toHotelDtoList(List<Hotel> hotels) {
        if ( hotels == null ) {
            return null;
        }

        List<HotelDto> list = new ArrayList<HotelDto>( hotels.size() );
        for ( Hotel hotel : hotels ) {
            list.add( toHotelDto( hotel ) );
        }

        return list;
    }

    @Override
    public List<RoomAvailabilityDto> toRoomAvailabilityDtoList(List<Room> rooms) {
        if ( rooms == null ) {
            return null;
        }

        List<RoomAvailabilityDto> list = new ArrayList<RoomAvailabilityDto>( rooms.size() );
        for ( Room room : rooms ) {
            list.add( toRoomAvailabilityDto( room ) );
        }

        return list;
    }
}
