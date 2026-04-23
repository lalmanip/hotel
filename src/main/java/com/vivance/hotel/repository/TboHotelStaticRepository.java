package com.vivance.hotel.repository;

import com.vivance.hotel.domain.entity.TboHotelStatic;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.util.Optional;

@Repository
public interface TboHotelStaticRepository extends JpaRepository<TboHotelStatic, Long> {
    Optional<TboHotelStatic> findByHotelCode(String hotelCode);
}

