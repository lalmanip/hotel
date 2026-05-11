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
        String getLatitude();
        String getLongitude();
    }

    @Query(
            value = """
                    SELECT t.hotel_code AS hotelCode,
                           t.hotel_name AS hotelName,
                           t.city_name  AS cityName,
                           t.country_name AS countryName,
                           t.latitude AS latitude,
                           t.longitude AS longitude
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
                           t.country_name AS countryName,
                           t.latitude AS latitude,
                           t.longitude AS longitude
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

    /**
     * Nearest hotels by coordinates (approx). Useful for map / nearby search.
     * Uses numeric cast because latitude/longitude are stored as strings.
     */
    @Query(
            value = """
                    SELECT t.hotel_code AS hotelCode,
                           t.hotel_name AS hotelName,
                           t.city_name  AS cityName,
                           t.country_name AS countryName,
                           t.latitude AS latitude,
                           t.longitude AS longitude
                    FROM tbo_hotels_static t
                    WHERE t.latitude IS NOT NULL AND t.latitude <> ''
                      AND t.longitude IS NOT NULL AND t.longitude <> ''
                      AND (:q IS NULL OR :q = '' OR t.hotel_name LIKE CONCAT('%', :q, '%'))
                    ORDER BY (
                        POW(CAST(t.latitude AS DECIMAL(10,6)) - :lat, 2) +
                        POW(CAST(t.longitude AS DECIMAL(10,6)) - :lng, 2)
                    ) ASC
                    LIMIT :limit
                    """,
            nativeQuery = true
    )
    List<StaticHotelLocationRow> searchNearestStaticHotels(
            @Param("q") String q,
            @Param("lat") double lat,
            @Param("lng") double lng,
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

    @Query("""
            select t.hotelCode from TboHotelStatic t
            order by t.hotelCode
            """)
    Page<String> findAllHotelCodes(Pageable pageable);

    /**
     * Hotel codes not yet successfully loaded into {@code tbo_hotel_static_details}
     * ({@code fetched_at} is null or row missing). Used to resume after a partial run.
     * <p>Native SQL with explicit {@code COLLATE} avoids MySQL error 1267 when the two tables
     * use different string collations (e.g. {@code utf8mb4_unicode_ci} vs {@code utf8mb4_0900_ai_ci}).
     */
    @Query(
            value = """
                    SELECT t.hotel_code FROM tbo_hotels_static t
                    WHERE NOT EXISTS (
                        SELECT 1 FROM tbo_hotel_static_details d
                        WHERE d.hotel_code COLLATE utf8mb4_unicode_ci = t.hotel_code COLLATE utf8mb4_unicode_ci
                          AND d.fetched_at IS NOT NULL
                    )
                    ORDER BY t.hotel_code
                    """,
            countQuery = """
                    SELECT COUNT(*) FROM tbo_hotels_static t
                    WHERE NOT EXISTS (
                        SELECT 1 FROM tbo_hotel_static_details d
                        WHERE d.hotel_code COLLATE utf8mb4_unicode_ci = t.hotel_code COLLATE utf8mb4_unicode_ci
                          AND d.fetched_at IS NOT NULL
                    )
                    """,
            nativeQuery = true
    )
    Page<String> findHotelCodesPendingDetailSync(Pageable pageable);

    @Query(
            value = """
                    SELECT COUNT(*) FROM tbo_hotels_static t
                    WHERE NOT EXISTS (
                        SELECT 1 FROM tbo_hotel_static_details d
                        WHERE d.hotel_code COLLATE utf8mb4_unicode_ci = t.hotel_code COLLATE utf8mb4_unicode_ci
                          AND d.fetched_at IS NOT NULL
                    )
                    """,
            nativeQuery = true
    )
    long countHotelCodesPendingDetailSync();

    @Query(
            value = """
                    SELECT t.hotel_code FROM tbo_hotels_static t
                    WHERE NOT EXISTS (
                        SELECT 1 FROM tbo_hotel_static_details d
                        WHERE d.hotel_code COLLATE utf8mb4_unicode_ci = t.hotel_code COLLATE utf8mb4_unicode_ci
                          AND d.fetched_at IS NOT NULL
                    )
                    AND UPPER(TRIM(t.city_name)) IN (:cityNamesUpper)
                    ORDER BY t.hotel_code
                    """,
            countQuery = """
                    SELECT COUNT(*) FROM tbo_hotels_static t
                    WHERE NOT EXISTS (
                        SELECT 1 FROM tbo_hotel_static_details d
                        WHERE d.hotel_code COLLATE utf8mb4_unicode_ci = t.hotel_code COLLATE utf8mb4_unicode_ci
                          AND d.fetched_at IS NOT NULL
                    )
                    AND UPPER(TRIM(t.city_name)) IN (:cityNamesUpper)
                    """,
            nativeQuery = true
    )
    Page<String> findHotelCodesPendingDetailSyncForCities(
            @Param("cityNamesUpper") List<String> cityNamesUpper,
            Pageable pageable);

    @Query(
            value = """
                    SELECT COUNT(*) FROM tbo_hotels_static t
                    WHERE NOT EXISTS (
                        SELECT 1 FROM tbo_hotel_static_details d
                        WHERE d.hotel_code COLLATE utf8mb4_unicode_ci = t.hotel_code COLLATE utf8mb4_unicode_ci
                          AND d.fetched_at IS NOT NULL
                    )
                    AND UPPER(TRIM(t.city_name)) IN (:cityNamesUpper)
                    """,
            nativeQuery = true
    )
    long countHotelCodesPendingDetailSyncForCities(@Param("cityNamesUpper") List<String> cityNamesUpper);
}

