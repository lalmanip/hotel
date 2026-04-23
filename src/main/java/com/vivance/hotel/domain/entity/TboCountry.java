package com.vivance.hotel.domain.entity;

import jakarta.persistence.*;
import lombok.*;

@Entity
@Table(name = "tbo_hotel_countries")
@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class TboCountry {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Column(name = "code", length = 5, nullable = false, unique = true)
    private String code;

    @Column(name = "name", length = 200, nullable = false)
    private String name;
}

