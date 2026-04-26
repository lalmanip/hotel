package com.vivance.hotel.repository;

import com.vivance.hotel.domain.entity.TboHotelStatic;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

import java.util.List;
import java.util.Optional;

@Repository
public interface TboHotelStaticRepository extends JpaRepository<TboHotelStatic, Long> {
    Optional<TboHotelStatic> findByHotelCode(String hotelCode);

    @Query("""
            select t.hotelCode from TboHotelStatic t
            where t.cityCode = :cityCode
            order by t.hotelCode
            """)
    Page<String> findHotelCodesByCityCode(
            @Param("cityCode") String cityCode,
            Pageable pageable);

    long countByCityCode(String cityCode);

    @Query("""
            select distinct t.hotelCode from TboHotelStatic t
            where t.cityCode = :cityCode
              and upper(trim(t.countryCode)) = upper(trim(:countryCode))
            order by t.hotelCode
            """)
    List<String> findDistinctHotelCodesByCityCodeAndCountryCode(
            @Param("cityCode") String cityCode,
            @Param("countryCode") String countryCode);
}

