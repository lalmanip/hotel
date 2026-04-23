package com.vivance.hotel.repository;

import com.vivance.hotel.domain.entity.TboCountry;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.util.Optional;

@Repository
public interface TboCountryRepository extends JpaRepository<TboCountry, Long> {
    Optional<TboCountry> findByCode(String code);
}

