# hotel
Hotel Booking portal
# =====================#
# AI prompt
# =====================#
You are a senior Java backend architect.

Build a production-grade backend for a Hotel Booking Platform using Java Spring Boot with the following requirements.

## 1. Tech Stack
- Java 21
- Spring Boot (latest stable)
- Spring Web, Spring Data JPA, Spring Security
- Hibernate ORM
- MySQL 
- Maven
- Lombok
- MapStruct (for DTO mapping)
- OpenFeign or WebClient for external API calls
- DB (for caching)
- Resilience4j (for circuit breaker, retries)
- Swagger/OpenAPI for API documentation

## 2. Architecture Requirements
Use CLEAN ARCHITECTURE / HEXAGONAL ARCHITECTURE:
- Controller layer (REST APIs)
- Service layer (business logic)
- Domain layer (core models)
- Infrastructure layer (external integrations like aggregators)
- Repository layer (JPA)

Follow SOLID principles strictly.

## 3. Core Features
Implement the following modules:

### A. Hotel Search
- Search hotels by city, date range, guests
- Normalize responses across aggregators
- Return unified response DTO

### B. Hotel Details
- Fetch hotel details (images, amenities, description)

### C. Room Availability & Pricing
- Real-time room availability
- Pricing per room

### D. Booking
- Create booking
- Store booking in DB
- Handle booking status (PENDING, CONFIRMED, FAILED)

### E. User Management
- JWT-based authentication
- Register/Login APIs

---

## 4. Aggregator Integration (VERY IMPORTANT)

Design system to support MULTIPLE aggregators like:
- TBO (current)
- Future aggregators (Expedia, Booking.com, etc.)

### Requirements:
- Create a common interface:
    HotelAggregatorService

- Each aggregator must implement:
    - searchHotels()
    - getHotelDetails()
    - getRoomAvailability()
    - bookHotel()

- Example:
    TboAggregatorService implements HotelAggregatorService

- Use Factory Pattern or Strategy Pattern to select aggregator dynamically.

- Configurable via application.yml:
    activeAggregators: [TBO]

- Add support for enabling multiple aggregators and merging results.

---

## 5. Database Design

Entities:
- User
- Hotel
- Room
- Booking
- AggregatorMapping (maps internal hotel ID to aggregator IDs)

Include proper relationships and indexing.

---

## 6. API Design

Expose REST endpoints:

/api/v1/hotels/search
/api/v1/hotels/{id}
/api/v1/hotels/{id}/availability
/api/v1/bookings
/api/v1/auth/register
/api/v1/auth/login

Use proper request/response DTOs.

---

## 7. Caching
- Cache hotel search results using Redis
- Cache hotel details

---

## 8. Resilience
- Add circuit breaker for aggregator APIs
- Retry logic
- Timeout handling

---

## 9. Error Handling
- Global exception handler
- Standard error response format

---

## 10. Security
- JWT authentication
- Role-based access (USER, ADMIN)

---

## 11. Logging & Monitoring
- Use SLF4J + Logback
- Add request/response logging

---

## 12. Deliverables

Generate FULL PROJECT CODE including:
- Project structure
- All Java classes
- application.yml
- pom.xml
- Sample controller implementations
- Aggregator mock implementation (TBO mock)
- DTOs, Entities, Repositories
- Security config
- Exception handling
- README with setup instructions

IMPORTANT:
- Code must be modular and extensible for adding new aggregators easily.
- Follow best practices used in real-world scalable systems.
- Include comments where needed.
- Avoid placeholder code — provide working implementation.

