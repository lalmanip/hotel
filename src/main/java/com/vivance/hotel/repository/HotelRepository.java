package com.vivance.hotel.repository;

import com.vivance.hotel.domain.entity.Hotel;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

import java.util.List;

@Repository
public interface HotelRepository extends JpaRepository<Hotel, Long> {

    List<Hotel> findByCityIgnoreCase(String city);

    @Query("SELECT h FROM Hotel h WHERE LOWER(h.city) = LOWER(:city) AND h.starRating >= :minStar")
    List<Hotel> findByCityAndMinStarRating(@Param("city") String city, @Param("minStar") int minStar);

    @Query("SELECT h FROM Hotel h WHERE LOWER(h.city) = LOWER(:city) OR LOWER(h.country) = LOWER(:location)")
    List<Hotel> findByLocation(@Param("location") String location);

    interface HotelLocationRow {
        Long getId();
        String getName();
        String getCity();
        String getCountry();
    }

    @Query(
            value = """
                    SELECT h.id AS id, h.name AS name, h.city AS city, h.country AS country
                    FROM hotels h
                    WHERE h.name LIKE CONCAT(:prefix, '%')
                    ORDER BY h.name ASC
                    LIMIT :limit
                    """,
            nativeQuery = true
    )
    List<HotelLocationRow> searchByNamePrefix(@Param("prefix") String prefix, @Param("limit") int limit);

    @Query(
            value = """
                    SELECT h.id AS id, h.name AS name, h.city AS city, h.country AS country
                    FROM hotels h
                    WHERE h.name LIKE CONCAT('%', :q, '%')
                      AND h.name NOT LIKE CONCAT(:q, '%')
                    ORDER BY h.name ASC
                    LIMIT :limit
                    """,
            nativeQuery = true
    )
    List<HotelLocationRow> searchByNameContainsExcludingPrefix(@Param("q") String q, @Param("limit") int limit);
}
