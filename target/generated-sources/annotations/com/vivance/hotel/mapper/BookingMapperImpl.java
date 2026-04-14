package com.vivance.hotel.mapper;

import com.vivance.hotel.domain.entity.Booking;
import com.vivance.hotel.domain.entity.Hotel;
import com.vivance.hotel.domain.entity.Room;
import com.vivance.hotel.dto.response.BookingDto;
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
public class BookingMapperImpl implements BookingMapper {

    @Override
    public BookingDto toBookingDto(Booking booking) {
        if ( booking == null ) {
            return null;
        }

        BookingDto.BookingDtoBuilder bookingDto = BookingDto.builder();

        bookingDto.hotelId( bookingHotelId( booking ) );
        bookingDto.hotelName( bookingHotelName( booking ) );
        bookingDto.roomId( bookingRoomId( booking ) );
        bookingDto.roomType( bookingRoomRoomType( booking ) );
        bookingDto.id( booking.getId() );
        bookingDto.bookingReference( booking.getBookingReference() );
        bookingDto.checkIn( booking.getCheckIn() );
        bookingDto.checkOut( booking.getCheckOut() );
        bookingDto.guests( booking.getGuests() );
        bookingDto.totalPrice( booking.getTotalPrice() );
        bookingDto.currency( booking.getCurrency() );
        bookingDto.status( booking.getStatus() );
        bookingDto.aggregatorBookingId( booking.getAggregatorBookingId() );
        bookingDto.specialRequests( booking.getSpecialRequests() );
        bookingDto.createdAt( booking.getCreatedAt() );

        return bookingDto.build();
    }

    @Override
    public List<BookingDto> toBookingDtoList(List<Booking> bookings) {
        if ( bookings == null ) {
            return null;
        }

        List<BookingDto> list = new ArrayList<BookingDto>( bookings.size() );
        for ( Booking booking : bookings ) {
            list.add( toBookingDto( booking ) );
        }

        return list;
    }

    private Long bookingHotelId(Booking booking) {
        if ( booking == null ) {
            return null;
        }
        Hotel hotel = booking.getHotel();
        if ( hotel == null ) {
            return null;
        }
        Long id = hotel.getId();
        if ( id == null ) {
            return null;
        }
        return id;
    }

    private String bookingHotelName(Booking booking) {
        if ( booking == null ) {
            return null;
        }
        Hotel hotel = booking.getHotel();
        if ( hotel == null ) {
            return null;
        }
        String name = hotel.getName();
        if ( name == null ) {
            return null;
        }
        return name;
    }

    private Long bookingRoomId(Booking booking) {
        if ( booking == null ) {
            return null;
        }
        Room room = booking.getRoom();
        if ( room == null ) {
            return null;
        }
        Long id = room.getId();
        if ( id == null ) {
            return null;
        }
        return id;
    }

    private String bookingRoomRoomType(Booking booking) {
        if ( booking == null ) {
            return null;
        }
        Room room = booking.getRoom();
        if ( room == null ) {
            return null;
        }
        String roomType = room.getRoomType();
        if ( roomType == null ) {
            return null;
        }
        return roomType;
    }
}
