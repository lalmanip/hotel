package com.vivance.hotel.repository;

import com.vivance.hotel.domain.entity.TboCity;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.util.Optional;

@Repository
public interface TboCityRepository extends JpaRepository<TboCity, Long> {
    Optional<TboCity> findByCode(String code);
}

