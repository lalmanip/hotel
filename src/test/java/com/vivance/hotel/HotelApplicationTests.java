package com.vivance.hotel;

import org.junit.jupiter.api.Test;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.test.context.ActiveProfiles;
import org.springframework.test.context.TestPropertySource;

@SpringBootTest
@ActiveProfiles("test")
@TestPropertySource(properties = {
    "spring.datasource.url=jdbc:h2:mem:testdb;DB_CLOSE_DELAY=-1",
    "spring.datasource.driver-class-name=org.h2.Driver",
    "spring.datasource.username=sa",
    "spring.datasource.password=",
    "spring.jpa.database-platform=org.hibernate.dialect.H2Dialect",
    "spring.jpa.hibernate.ddl-auto=create-drop",
    "spring.data.redis.host=localhost",
    "spring.data.redis.port=6379",
    "jwt.secret=5367566B59703373367639792F423F4528482B4D6251655468576D5A71347437",
    "jwt.expiration-ms=86400000",
    "hotel.aggregators.active=TBO",
    "hotel.aggregators.tbo.base-url=http://localhost",
    "hotel.aggregators.tbo.api-key=test-key",
    "hotel.aggregators.tbo.api-secret=test-secret"
})
class HotelApplicationTests {

    @Test
    void contextLoads() {
        // Verifies that the Spring application context starts successfully
    }
}
