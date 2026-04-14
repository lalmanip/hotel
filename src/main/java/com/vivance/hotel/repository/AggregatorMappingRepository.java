package com.vivance.hotel.repository;

import com.vivance.hotel.domain.entity.AggregatorMapping;
import com.vivance.hotel.domain.enums.AggregatorType;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.util.List;
import java.util.Optional;

@Repository
public interface AggregatorMappingRepository extends JpaRepository<AggregatorMapping, Long> {

    Optional<AggregatorMapping> findByHotelIdAndAggregatorType(Long hotelId, AggregatorType type);

    List<AggregatorMapping> findByHotelId(Long hotelId);

    Optional<AggregatorMapping> findByExternalHotelIdAndAggregatorType(String externalHotelId, AggregatorType type);
}
