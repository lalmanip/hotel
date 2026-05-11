package com.vivance.hotel.domain.entity;

import jakarta.persistence.*;
import lombok.*;
import org.hibernate.annotations.CreationTimestamp;
import org.hibernate.annotations.UpdateTimestamp;

import java.time.LocalDateTime;

@Entity
@Table(
        name = "tbo_hotel_static_details",
        indexes = {
                @Index(name = "idx_tbo_hotel_static_details_city_id", columnList = "city_id"),
                @Index(name = "idx_tbo_hotel_static_details_country_code", columnList = "country_code")
        }
)
@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class TboHotelStaticDetails {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Column(name = "hotel_code", length = 50, nullable = false, unique = true)
    private String hotelCode;

    @Column(name = "hotel_name", length = 500)
    private String hotelName;

    @Column(name = "description", columnDefinition = "LONGTEXT")
    private String description;

    @Column(name = "hotel_facilities_json", columnDefinition = "json")
    private String hotelFacilitiesJson;

    @Column(name = "attractions_json", columnDefinition = "json")
    private String attractionsJson;

    @Column(name = "image_url", length = 2048)
    private String imageUrl;

    @Column(name = "images_json", columnDefinition = "json")
    private String imagesJson;

    @Column(name = "address", length = 1000)
    private String address;

    @Column(name = "pin_code", length = 32)
    private String pinCode;

    @Column(name = "city_id", length = 32)
    private String cityId;

    @Column(name = "city_name", length = 200)
    private String cityName;

    @Column(name = "country_name", length = 200)
    private String countryName;

    @Column(name = "country_code", length = 5)
    private String countryCode;

    @Column(name = "phone_number", length = 64)
    private String phoneNumber;

    @Column(name = "email", length = 320)
    private String email;

    @Column(name = "hotel_website_url", length = 2048)
    private String hotelWebsiteUrl;

    @Column(name = "fax_number", length = 64)
    private String faxNumber;

    @Column(name = "map_coordinates", length = 64)
    private String mapCoordinates;

    @Column(name = "hotel_rating")
    private Integer hotelRating;

    @Column(name = "check_in_time", length = 32)
    private String checkInTime;

    @Column(name = "check_out_time", length = 32)
    private String checkOutTime;

    @Column(name = "hotel_fees_json", columnDefinition = "json")
    private String hotelFeesJson;

    @Column(name = "fetched_at")
    private LocalDateTime fetchedAt;

    @CreationTimestamp
    @Column(name = "created_at", updatable = false)
    private LocalDateTime createdAt;

    @UpdateTimestamp
    @Column(name = "updated_at")
    private LocalDateTime updatedAt;
}
