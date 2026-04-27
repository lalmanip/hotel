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

    interface StaticHotelLocationRow {
        String getHotelCode();
        String getHotelName();
        String getCityName();
        String getCountryName();
    }

    @Query(
            value = """
                    SELECT t.hotel_code AS hotelCode,
                           t.hotel_name AS hotelName,
                           t.city_name  AS cityName,
                           t.country_name AS countryName
                    FROM tbo_hotels_static t
                    WHERE t.hotel_name LIKE CONCAT(:prefix, '%')
                    ORDER BY t.hotel_name ASC
                    LIMIT :limit
                    """,
            nativeQuery = true
    )
    List<StaticHotelLocationRow> searchStaticHotelsByNamePrefix(
            @Param("prefix") String prefix,
            @Param("limit") int limit);

    @Query(
            value = """
                    SELECT t.hotel_code AS hotelCode,
                           t.hotel_name AS hotelName,
                           t.city_name  AS cityName,
                           t.country_name AS countryName
                    FROM tbo_hotels_static t
                    WHERE t.hotel_name LIKE CONCAT('%', :q, '%')
                      AND t.hotel_name NOT LIKE CONCAT(:q, '%')
                    ORDER BY t.hotel_name ASC
                    LIMIT :limit
                    """,
            nativeQuery = true
    )
    List<StaticHotelLocationRow> searchStaticHotelsByNameContainsExcludingPrefix(
            @Param("q") String q,
            @Param("limit") int limit);

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

