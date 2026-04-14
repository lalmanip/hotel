package com.vivance.hotel.domain.entity;

import com.vivance.hotel.domain.enums.AggregatorType;
import com.vivance.hotel.domain.enums.BookingStatus;
import jakarta.persistence.*;
import lombok.*;
import org.hibernate.annotations.CreationTimestamp;
import org.hibernate.annotations.UpdateTimestamp;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.time.LocalDateTime;

@Entity
@Table(
    name = "bookings",
    indexes = {
        @Index(name = "idx_booking_user", columnList = "user_id"),
        @Index(name = "idx_booking_status", columnList = "status"),
        @Index(name = "idx_booking_reference", columnList = "booking_reference", unique = true)
    }
)
@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class Booking {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    /** Unique human-readable reference, e.g. VH-20240413-00001 */
    @Column(name = "booking_reference", unique = true, nullable = false, length = 30)
    private String bookingReference;

    @ManyToOne(fetch = FetchType.LAZY, optional = false)
    @JoinColumn(name = "user_id", nullable = false)
    private User user;

    @ManyToOne(fetch = FetchType.LAZY, optional = false)
    @JoinColumn(name = "hotel_id", nullable = false)
    private Hotel hotel;

    @ManyToOne(fetch = FetchType.LAZY, optional = false)
    @JoinColumn(name = "room_id", nullable = false)
    private Room room;

    @Column(nullable = false)
    private LocalDate checkIn;

    @Column(nullable = false)
    private LocalDate checkOut;

    @Column(nullable = false)
    private Integer guests;

    @Column(nullable = false, precision = 12, scale = 2)
    private BigDecimal totalPrice;

    @Column(length = 3)
    @Builder.Default
    private String currency = "USD";

    @Enumerated(EnumType.STRING)
    @Column(nullable = false, length = 20)
    @Builder.Default
    private BookingStatus status = BookingStatus.PENDING;

    @Enumerated(EnumType.STRING)
    @Column(length = 30)
    private AggregatorType aggregatorType;

    /** Aggregator's own booking reference */
    @Column(length = 100)
    private String aggregatorBookingId;

    /** Reason if booking failed */
    @Column(columnDefinition = "TEXT")
    private String failureReason;

    @Column(length = 200)
    private String specialRequests;

    // ── TBO session fields — stored so the aggregator call can be reconstructed ─

    @Column(length = 100)
    private String tboTraceId;

    @Column(length = 20)
    private String tboResultIndex;

    @Column(length = 20)
    private String tboRoomIndex;

    @Column(length = 200)
    private String tboRoomTypeCode;

    @Column(length = 300)
    private String tboRatePlanCode;

    @Column(length = 5)
    @Builder.Default
    private String guestNationality = "IN";

    @CreationTimestamp
    @Column(updatable = false)
    private LocalDateTime createdAt;

    @UpdateTimestamp
    private LocalDateTime updatedAt;
}
