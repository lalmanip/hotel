package com.vivance.hotel.repository;

import com.vivance.hotel.domain.entity.Booking;
import com.vivance.hotel.domain.enums.BookingStatus;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.util.List;
import java.util.Optional;

@Repository
public interface BookingRepository extends JpaRepository<Booking, Long> {

    List<Booking> findByUserId(Integer userId);

    List<Booking> findByUserIdAndStatus(Integer userId, BookingStatus status);

    Optional<Booking> findByBookingReference(String bookingReference);

    boolean existsByBookingReference(String bookingReference);
}
