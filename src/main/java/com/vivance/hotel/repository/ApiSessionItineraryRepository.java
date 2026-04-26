package com.vivance.hotel.repository;

import com.vivance.hotel.domain.entity.ApiSessionItinerary;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.util.Optional;

@Repository
public interface ApiSessionItineraryRepository extends JpaRepository<ApiSessionItinerary, Long> {
    Optional<ApiSessionItinerary> findTopByTraceIdOrderByIdDesc(String traceId);
}

