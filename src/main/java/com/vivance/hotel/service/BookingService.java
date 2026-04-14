package com.vivance.hotel.service;

import com.vivance.hotel.domain.entity.Booking;
import com.vivance.hotel.domain.entity.Hotel;
import com.vivance.hotel.domain.entity.Room;
import com.vivance.hotel.domain.entity.User;
import com.vivance.hotel.domain.enums.AggregatorType;
import com.vivance.hotel.domain.enums.BookingStatus;
import com.vivance.hotel.dto.request.BookingRequest;
import com.vivance.hotel.dto.response.BookingDto;
import com.vivance.hotel.exception.BookingException;
import com.vivance.hotel.exception.HotelNotFoundException;
import com.vivance.hotel.exception.ResourceNotFoundException;
import com.vivance.hotel.infrastructure.aggregator.AggregatorFactory;
import com.vivance.hotel.infrastructure.aggregator.HotelAggregatorService;
import com.vivance.hotel.mapper.BookingMapper;
import com.vivance.hotel.repository.AggregatorMappingRepository;
import com.vivance.hotel.repository.BookingRepository;
import com.vivance.hotel.repository.HotelRepository;
import com.vivance.hotel.repository.RoomRepository;
import com.vivance.hotel.repository.UserRepository;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.security.core.userdetails.UsernameNotFoundException;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.time.format.DateTimeFormatter;
import java.time.temporal.ChronoUnit;
import java.util.List;
import java.util.UUID;

@Slf4j
@Service
@RequiredArgsConstructor
public class BookingService {

    private final BookingRepository bookingRepository;
    private final HotelRepository hotelRepository;
    private final RoomRepository roomRepository;
    private final UserRepository userRepository;
    private final AggregatorMappingRepository aggregatorMappingRepository;
    private final AggregatorFactory aggregatorFactory;
    private final BookingMapper bookingMapper;

    /**
     * Creates a new booking:
     * 1. Validate request (dates, room capacity)
     * 2. Persist booking with PENDING status
     * 3. Attempt aggregator confirmation
     * 4. Update status to CONFIRMED or FAILED
     */
    @Transactional
    public BookingDto createBooking(BookingRequest request, String userEmail) {
        // Validate dates
        if (!request.getCheckOut().isAfter(request.getCheckIn())) {
            throw new BookingException("Check-out date must be after check-in date");
        }

        User user = userRepository.findByEmail(userEmail)
                .orElseThrow(() -> new UsernameNotFoundException("User not found: " + userEmail));

        Hotel hotel = hotelRepository.findById(request.getHotelId())
                .orElseThrow(() -> new HotelNotFoundException(request.getHotelId()));

        Room room = roomRepository.findById(request.getRoomId())
                .orElseThrow(() -> new ResourceNotFoundException("Room", request.getRoomId()));

        if (!room.getHotel().getId().equals(hotel.getId())) {
            throw new BookingException("Room does not belong to the specified hotel");
        }

        if (room.getMaxOccupancy() < request.getGuests()) {
            throw new BookingException(
                    "Room capacity exceeded. Max occupancy: " + room.getMaxOccupancy());
        }

        // Check for conflicting bookings
        List<Room> available = roomRepository.findAvailableRooms(
                hotel.getId(), request.getCheckIn(), request.getCheckOut(), request.getGuests());
        boolean roomAvailable = available.stream().anyMatch(r -> r.getId().equals(room.getId()));
        if (!roomAvailable) {
            throw new BookingException("Room is not available for the selected dates");
        }

        long nights = ChronoUnit.DAYS.between(request.getCheckIn(), request.getCheckOut());
        BigDecimal totalPrice = room.getPricePerNight().multiply(BigDecimal.valueOf(nights));

        // Create booking with PENDING status
        Booking booking = Booking.builder()
                .bookingReference(generateReference())
                .user(user)
                .hotel(hotel)
                .room(room)
                .checkIn(request.getCheckIn())
                .checkOut(request.getCheckOut())
                .guests(request.getGuests())
                .totalPrice(totalPrice)
                .currency(room.getCurrency())
                .status(BookingStatus.PENDING)
                .specialRequests(request.getSpecialRequests())
                // TBO session fields — persist so the aggregator call can use them
                .tboTraceId(request.getTraceId())
                .tboResultIndex(request.getResultIndex())
                .tboRoomIndex(request.getTboRoomIndex())
                .tboRoomTypeCode(request.getRoomTypeCode())
                .tboRatePlanCode(request.getRatePlanCode())
                .guestNationality(request.getGuestNationality() != null
                        ? request.getGuestNationality() : "IN")
                .build();

        booking = bookingRepository.save(booking);
        log.info("Created PENDING booking ref={} for user={}", booking.getBookingReference(), userEmail);

        // Attempt aggregator booking confirmation
        confirmWithAggregator(booking, user, hotel, room);
        return bookingMapper.toBookingDto(booking);
    }

    @Transactional(readOnly = true)
    public List<BookingDto> getUserBookings(String userEmail) {
        User user = userRepository.findByEmail(userEmail)
                .orElseThrow(() -> new UsernameNotFoundException("User not found: " + userEmail));
        return bookingMapper.toBookingDtoList(bookingRepository.findByUserId(user.getId()));
    }

    @Transactional(readOnly = true)
    public BookingDto getBooking(Long bookingId, String userEmail) {
        Booking booking = bookingRepository.findById(bookingId)
                .orElseThrow(() -> new ResourceNotFoundException("Booking", bookingId));

        if (!booking.getUser().getEmail().equals(userEmail)) {
            throw new BookingException("You are not authorized to view this booking");
        }
        return bookingMapper.toBookingDto(booking);
    }

    @Transactional
    public BookingDto cancelBooking(Long bookingId, String userEmail) {
        Booking booking = bookingRepository.findById(bookingId)
                .orElseThrow(() -> new ResourceNotFoundException("Booking", bookingId));

        if (!booking.getUser().getEmail().equals(userEmail)) {
            throw new BookingException("You are not authorized to cancel this booking");
        }
        if (booking.getStatus() == BookingStatus.CANCELLED) {
            throw new BookingException("Booking is already cancelled");
        }
        if (booking.getCheckIn().isBefore(LocalDate.now())) {
            throw new BookingException("Cannot cancel a booking that has already started");
        }

        booking.setStatus(BookingStatus.CANCELLED);
        log.info("Booking cancelled: ref={}", booking.getBookingReference());
        return bookingMapper.toBookingDto(bookingRepository.save(booking));
    }

    // ─── Private helpers ─────────────────────────────────────────────────────

    private void confirmWithAggregator(Booking booking, User user, Hotel hotel, Room room) {
        aggregatorMappingRepository.findByHotelId(hotel.getId()).stream().findFirst()
                .ifPresentOrElse(mapping -> {
                    try {
                        AggregatorType type = mapping.getAggregatorType();
                        HotelAggregatorService aggregator = aggregatorFactory.getAggregator(type);

                        BookingDto aggConfirmation = aggregator.bookHotel(
                                mapping.getExternalHotelId(),
                                booking.getTboRoomIndex() != null
                                        ? booking.getTboRoomIndex()
                                        : String.valueOf(room.getId()),
                                booking.getCheckIn(),
                                booking.getCheckOut(),
                                booking.getGuests(),
                                user.getFullName(),
                                user.getEmail(),
                                booking.getTboResultIndex(),
                                booking.getTboTraceId(),
                                booking.getTboRoomTypeCode(),
                                booking.getTboRatePlanCode(),
                                booking.getGuestNationality()
                        );

                        booking.setStatus(BookingStatus.CONFIRMED);
                        booking.setAggregatorType(type);
                        booking.setAggregatorBookingId(aggConfirmation.getAggregatorBookingId());
                        bookingRepository.save(booking);
                        log.info("Booking CONFIRMED via {}: ref={}, aggRef={}",
                                type, booking.getBookingReference(), aggConfirmation.getAggregatorBookingId());

                    } catch (Exception e) {
                        log.error("Aggregator booking failed: {}", e.getMessage());
                        booking.setStatus(BookingStatus.FAILED);
                        booking.setFailureReason(e.getMessage());
                        bookingRepository.save(booking);
                    }
                }, () -> {
                    // No aggregator mapping: treat as directly confirmed in local system
                    booking.setStatus(BookingStatus.CONFIRMED);
                    bookingRepository.save(booking);
                    log.info("Booking CONFIRMED locally (no aggregator): ref={}", booking.getBookingReference());
                });
    }

    private String generateReference() {
        String date = LocalDate.now().format(DateTimeFormatter.BASIC_ISO_DATE);
        String uid = UUID.randomUUID().toString().substring(0, 6).toUpperCase();
        return "VH-" + date + "-" + uid;
    }
}
