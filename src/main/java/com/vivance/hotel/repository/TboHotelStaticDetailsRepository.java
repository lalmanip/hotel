package com.vivance.hotel.repository;

import com.vivance.hotel.domain.entity.TboHotelStaticDetails;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.util.Collection;
import java.util.List;

@Repository
public interface TboHotelStaticDetailsRepository extends JpaRepository<TboHotelStaticDetails, Long> {

    List<TboHotelStaticDetails> findAllByHotelCodeIn(Collection<String> hotelCodes);
}
