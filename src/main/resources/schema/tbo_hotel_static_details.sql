-- Static hotel detail content from TBO Hoteldetails API (one row per hotel_code).
-- Run manually or via your migration process (JPA ddl-auto is none in this project).
-- Requires MySQL 5.7.8+ / MariaDB 10.2.7+ for JSON columns; otherwise replace JSON with LONGTEXT and store serialized JSON.

CREATE TABLE IF NOT EXISTS tbo_hotel_static_details (
    id                   BIGINT UNSIGNED NOT NULL AUTO_INCREMENT,
    hotel_code           VARCHAR(50)     NOT NULL COMMENT 'TBO HotelCode',
    hotel_name           VARCHAR(500)    NULL,
    description          LONGTEXT        NULL,
    hotel_facilities_json JSON           NULL COMMENT 'TBO HotelFacilities array',
    attractions_json     JSON            NULL COMMENT 'TBO Attractions object',
    image_url            VARCHAR(2048)   NULL COMMENT 'TBO single Image field',
    images_json          JSON            NULL COMMENT 'TBO Images array',
    address              VARCHAR(1000)   NULL,
    pin_code             VARCHAR(32)     NULL,
    city_id              VARCHAR(32)     NULL COMMENT 'TBO CityId',
    city_name            VARCHAR(200)    NULL,
    country_name         VARCHAR(200)    NULL,
    country_code         VARCHAR(5)      NULL,
    phone_number         VARCHAR(64)     NULL,
    email                VARCHAR(320)    NULL,
    hotel_website_url    VARCHAR(2048)   NULL,
    fax_number           VARCHAR(64)     NULL,
    map_coordinates      VARCHAR(64)     NULL COMMENT 'TBO Map: lat|long',
    hotel_rating         TINYINT UNSIGNED NULL,
    check_in_time        VARCHAR(32)     NULL,
    check_out_time       VARCHAR(32)     NULL,
    hotel_fees_json      JSON            NULL COMMENT 'TBO HotelFees object',
    fetched_at           DATETIME(3)     NULL COMMENT 'Last successful refresh from TBO',
    created_at           DATETIME(3)     NOT NULL DEFAULT CURRENT_TIMESTAMP(3),
    updated_at           DATETIME(3)     NOT NULL DEFAULT CURRENT_TIMESTAMP(3) ON UPDATE CURRENT_TIMESTAMP(3),
    PRIMARY KEY (id),
    UNIQUE KEY uk_tbo_hotel_static_details_hotel_code (hotel_code),
    KEY idx_tbo_hotel_static_details_city_id (city_id),
    KEY idx_tbo_hotel_static_details_country_code (country_code)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_unicode_ci;
