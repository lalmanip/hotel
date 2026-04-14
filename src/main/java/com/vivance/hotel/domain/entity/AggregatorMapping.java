package com.vivance.hotel.domain.entity;

import com.vivance.hotel.domain.enums.AggregatorType;
import jakarta.persistence.*;
import lombok.*;

/**
 * Maps an internal Hotel ID to the hotel's ID on an external aggregator.
 * Allows us to resolve the correct aggregator hotel ID when booking.
 */
@Entity
@Table(
    name = "aggregator_mappings",
    uniqueConstraints = {
        @UniqueConstraint(
            name = "uq_aggregator_hotel",
            columnNames = {"hotel_id", "aggregator_type"}
        )
    },
    indexes = {
        @Index(name = "idx_agg_mapping_hotel", columnList = "hotel_id"),
        @Index(name = "idx_agg_mapping_ext_id", columnList = "external_hotel_id")
    }
)
@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class AggregatorMapping {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @ManyToOne(fetch = FetchType.LAZY, optional = false)
    @JoinColumn(name = "hotel_id", nullable = false)
    private Hotel hotel;

    @Enumerated(EnumType.STRING)
    @Column(name = "aggregator_type", nullable = false, length = 30)
    private AggregatorType aggregatorType;

    /** The hotel's ID as known by the aggregator */
    @Column(name = "external_hotel_id", nullable = false, length = 100)
    private String externalHotelId;

    /** Additional metadata from aggregator (JSON or key=value) */
    @Column(columnDefinition = "TEXT")
    private String metadata;
}
