package com.vivance.hotel.dto.response;

import com.vivance.hotel.domain.enums.BookingStatus;
import lombok.Builder;
import lombok.Data;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.time.LocalDateTime;

@Data
@Builder
public class BookingDto {

    private Long id;
    private String bookingReference;
    private Long hotelId;
    private String hotelName;
    private Long roomId;
    private String roomType;
    private LocalDate checkIn;
    private LocalDate checkOut;
    private Integer guests;
    private BigDecimal totalPrice;
    private String currency;
    private BookingStatus status;
    private String aggregatorBookingId;
    private String specialRequests;
    private LocalDateTime createdAt;
}
