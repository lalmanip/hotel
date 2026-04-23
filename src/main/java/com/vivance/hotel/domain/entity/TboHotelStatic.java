package com.vivance.hotel.domain.entity;

import jakarta.persistence.*;
import lombok.*;

@Entity
@Table(
        name = "tbo_hotels_static",
        indexes = {
                @Index(name = "idx_tbo_hotel_city", columnList = "city_code"),
                @Index(name = "idx_tbo_hotel_code", columnList = "hotel_code", unique = true)
        }
)
@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class TboHotelStatic {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Column(name = "hotel_code", length = 50, nullable = false, unique = true)
    private String hotelCode;

    @Column(name = "hotel_name", length = 500)
    private String hotelName;

    @Column(name = "city_code", length = 20, nullable = false)
    private String cityCode;

    @Column(name = "city_name", length = 200)
    private String cityName;

    @Column(name = "country_code", length = 5)
    private String countryCode;

    @Column(name = "country_name", length = 200)
    private String countryName;

    @Column(name = "hotel_rating", length = 50)
    private String hotelRating;

    @Column(name = "latitude", length = 50)
    private String latitude;

    @Column(name = "longitude", length = 50)
    private String longitude;

    @Column(name = "address", length = 1000)
    private String address;
}

