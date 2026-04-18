package com.vivance.hotel;

import org.junit.jupiter.api.Test;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.test.context.ActiveProfiles;
import org.springframework.test.context.TestPropertySource;

@SpringBootTest
@ActiveProfiles("test")
@TestPropertySource(properties = {
    // ── Database (H2 in-memory) ─────────────────────────────────────────
    "spring.datasource.url=jdbc:h2:mem:testdb;DB_CLOSE_DELAY=-1",
    "spring.datasource.driver-class-name=org.h2.Driver",
    "spring.datasource.username=sa",
    "spring.datasource.password=",
    "spring.jpa.database-platform=org.hibernate.dialect.H2Dialect",
    "spring.jpa.hibernate.ddl-auto=create-drop",
    // ── Cache — disable Redis so no connection is attempted ─────────────
    "spring.cache.type=none",
    "spring.data.redis.host=localhost",
    "spring.data.redis.port=6379",
    // ── JWT ─────────────────────────────────────────────────────────────
    "jwt.secret=5367566B59703373367639792F423F4528482B4D6251655468576D5A71347437",
    "jwt.expiration-ms=86400000",
    // ── Aggregator ───────────────────────────────────────────────────────
    "hotel.aggregators.active=TBO",
    "hotel.aggregators.tbo.base-url=http://localhost",
    "hotel.aggregators.tbo.internal-base-url=http://localhost",
    "hotel.aggregators.tbo.timeout-seconds=5",
    "hotel.aggregators.tbo.auth-url=http://localhost/auth",
    "hotel.aggregators.tbo.client-id=test-client",
    "hotel.aggregators.tbo.user-name=test-user",
    "hotel.aggregators.tbo.password=test-pass",
    "hotel.aggregators.tbo.end-user-ip=127.0.0.1"
})
class HotelApplicationTests {

    @Test
    void contextLoads() {
        // Verifies that the Spring application context starts successfully
    }
}
