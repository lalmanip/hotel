package com.vivance.hotel.domain.entity;

import jakarta.persistence.*;
import lombok.*;

@Entity
@Table(
        name = "tbo_hotel_cities",
        indexes = {
                @Index(name = "idx_tbo_city_country", columnList = "country_code"),
                @Index(name = "idx_tbo_city_code", columnList = "code", unique = true),
                @Index(name = "idx_tbo_city_name", columnList = "name")
        }
)
@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class TboHotelCity {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Column(name = "code", length = 20, nullable = false, unique = true)
    private String code;

    @Column(name = "country_code", length = 5, nullable = false)
    private String countryCode;

    @Column(name = "name", length = 255, nullable = false)
    private String name;
}
