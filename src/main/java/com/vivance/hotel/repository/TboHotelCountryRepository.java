package com.vivance.hotel.repository;

import com.vivance.hotel.domain.entity.TboHotelCountry;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.util.List;

@Repository
public interface TboHotelCountryRepository extends JpaRepository<TboHotelCountry, Long> {

    List<TboHotelCountry> findAllByOrderByNameAsc();
}
