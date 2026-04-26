package com.vivance.hotel.repository;

import com.vivance.hotel.domain.entity.TboHotelCity;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

import java.util.List;

@Repository
public interface TboHotelCityRepository extends JpaRepository<TboHotelCity, Long> {

    List<TboHotelCity> findByCountryCodeIgnoreCaseOrderByNameAsc(String countryCode);

    interface CityLocationRow {
        String getCode();
        String getName();
        String getCountryCode();
    }

    @Query(
            value = """
                    SELECT c.code AS code, c.name AS name, c.country_code AS countryCode
                    FROM tbo_hotel_cities c
                    WHERE c.name LIKE CONCAT(:prefix, '%')
                    ORDER BY c.name ASC
                    LIMIT :limit
                    """,
            nativeQuery = true
    )
    List<CityLocationRow> searchByNamePrefix(@Param("prefix") String prefix, @Param("limit") int limit);

    @Query(
            value = """
                    SELECT c.code AS code, c.name AS name, c.country_code AS countryCode
                    FROM tbo_hotel_cities c
                    WHERE c.name LIKE CONCAT('%', :q, '%')
                      AND c.name NOT LIKE CONCAT(:q, '%')
                    ORDER BY c.name ASC
                    LIMIT :limit
                    """,
            nativeQuery = true
    )
    List<CityLocationRow> searchByNameContainsExcludingPrefix(@Param("q") String q, @Param("limit") int limit);
}
