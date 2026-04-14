package com.vivance.hotel.repository;

import com.vivance.hotel.domain.entity.Room;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

import java.time.LocalDate;
import java.util.List;

@Repository
public interface RoomRepository extends JpaRepository<Room, Long> {

    List<Room> findByHotelIdAndAvailableTrue(Long hotelId);

    /**
     * Returns rooms for a hotel that have no overlapping confirmed bookings
     * for the requested date range.
     */
    @Query("""
        SELECT r FROM Room r
        WHERE r.hotel.id = :hotelId
          AND r.available = true
          AND r.maxOccupancy >= :guests
          AND NOT EXISTS (
              SELECT b FROM Booking b
              WHERE b.room.id = r.id
                AND b.status IN ('PENDING', 'CONFIRMED')
                AND b.checkIn < :checkOut
                AND b.checkOut > :checkIn
          )
        """)
    List<Room> findAvailableRooms(
            @Param("hotelId") Long hotelId,
            @Param("checkIn") LocalDate checkIn,
            @Param("checkOut") LocalDate checkOut,
            @Param("guests") int guests
    );
}
