package com.vivance.hotel.domain.entity;

import jakarta.persistence.*;
import lombok.*;
import org.hibernate.annotations.CreationTimestamp;
import org.hibernate.annotations.UpdateTimestamp;

import java.math.BigDecimal;
import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.List;

@Entity
@Table(
    name = "rooms",
    indexes = {
        @Index(name = "idx_room_hotel", columnList = "hotel_id"),
        @Index(name = "idx_room_type", columnList = "room_type")
    }
)
@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class Room {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @ManyToOne(fetch = FetchType.LAZY, optional = false)
    @JoinColumn(name = "hotel_id", nullable = false)
    private Hotel hotel;

    @Column(nullable = false, length = 100)
    private String roomType;

    @Column(nullable = false)
    private Integer maxOccupancy;

    @Column(nullable = false, precision = 10, scale = 2)
    private BigDecimal pricePerNight;

    @Column(length = 3)
    @Builder.Default
    private String currency = "USD";

    @Column(columnDefinition = "TEXT")
    private String description;

    /** Comma-separated amenity list */
    @Column(columnDefinition = "TEXT")
    private String amenities;

    /** Comma-separated image URLs */
    @Column(columnDefinition = "TEXT")
    private String imageUrls;

    @Column(nullable = false)
    @Builder.Default
    private boolean available = true;

    @OneToMany(mappedBy = "room", cascade = CascadeType.ALL, fetch = FetchType.LAZY)
    @Builder.Default
    private List<Booking> bookings = new ArrayList<>();

    @CreationTimestamp
    @Column(updatable = false)
    private LocalDateTime createdAt;

    @UpdateTimestamp
    private LocalDateTime updatedAt;
}
