package com.vivance.hotel.controller;

import com.vivance.hotel.domain.enums.AggregatorType;
import com.vivance.hotel.dto.request.HotelSearchV1Request;
import com.vivance.hotel.dto.request.HotelPreBookRequest;
import com.vivance.hotel.dto.request.HotelBookRequest;
import com.vivance.hotel.dto.response.ApiResponse;
import com.vivance.hotel.dto.response.HotelCityDto;
import com.vivance.hotel.dto.response.HotelCountryDto;
import com.vivance.hotel.dto.response.HotelDetailDto;
import com.vivance.hotel.dto.response.HotelDto;
import com.vivance.hotel.dto.response.HotelSearchV1Response;
import com.vivance.hotel.dto.response.RoomAvailabilityDto;
import com.vivance.hotel.infrastructure.aggregator.tbo.dto.TboAffiliatePreBookResponse;
import com.vivance.hotel.infrastructure.aggregator.tbo.dto.TboAffiliateBookRequest;
import com.vivance.hotel.infrastructure.aggregator.tbo.dto.TboAffiliateSearchResponse;
import com.vivance.hotel.infrastructure.aggregator.tbo.dto.TboBookResponse;
import com.vivance.hotel.infrastructure.aggregator.tbo.dto.TboGetBookingDetailRequest;
import com.vivance.hotel.infrastructure.aggregator.tbo.dto.TboGetBookingDetailResponse;
import com.vivance.hotel.config.RequestLoggingConfig;
import com.vivance.hotel.domain.entity.ApiAccessLog;
import com.vivance.hotel.service.HotelCityService;
import com.vivance.hotel.service.HotelCountryService;
import com.vivance.hotel.service.HotelSearchV1Service;
import com.vivance.hotel.service.HotelService;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.Parameter;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.validation.Valid;
import jakarta.validation.constraints.Min;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.format.annotation.DateTimeFormat;
import org.springframework.http.ResponseEntity;
import org.springframework.validation.annotation.Validated;
import org.springframework.web.bind.annotation.*;

import jakarta.servlet.http.HttpServletRequest;
import java.time.LocalDate;
import java.util.List;

@Slf4j
@Validated
@RestController
@RequestMapping({"/api/hotels", "/api/v1/hotels"})
@RequiredArgsConstructor
@Tag(name = "Hotels", description = "Hotel search, details, and room availability")
public class HotelController {

    private final HotelService hotelService;
    private final HotelSearchV1Service hotelSearchV1Service;
    private final HotelCountryService hotelCountryService;
    private final HotelCityService hotelCityService;

    /**
     * POST /api/v1/hotels/search
     *
     * <p>For TBO, pass {@code HotelCodes}, or {@code Citycode} + {@code CountryCode} to resolve hotel codes
     * from {@code tbo_hotels_static}
     * and run parallel affiliate Search (one TBO call per hotel code; first response within
     * {@code ResponseTime} budget, remaining calls continue in the background and are cached for repeat searches).
     * The response includes {@code traceId} and {@code resultIndex} per hotel when applicable —
     * store these for hotel-detail and availability calls.
     */
    @PostMapping("/search")
    @Operation(summary = "Search hotels by city, dates, and guest count",
               description = "Searches TBO by hotel codes. If destinationId is provided, hotel codes are paged from DB (tbo_hotels_static.city_code) then searched in TBO in chunks.")
    public ResponseEntity<ApiResponse<TboAffiliateSearchResponse>> searchHotels(
            @Valid @RequestBody HotelSearchV1Request request) {

        TboAffiliateSearchResponse resp = hotelSearchV1Service.searchRaw(request);
        return ResponseEntity.ok(ApiResponse.success(resp));
    }

    /**
     * Optional mapped/normalized response for UI use-cases.
     * If you only want raw TBO, use {@code POST /api/v1/hotels/search}.
     */
    @PostMapping("/search/mapped")
    @Operation(summary = "Search hotels (mapped response)",
            description = "Returns a simplified mapped response (name/price/amenities). Prefer /search for raw TBO payload.")
    public ResponseEntity<ApiResponse<HotelSearchV1Response>> searchHotelsMapped(
            @Valid @RequestBody HotelSearchV1Request request) {
        HotelSearchV1Response resp = hotelSearchV1Service.search(request);
        return ResponseEntity.ok(ApiResponse.success(resp));
    }

    /**
     * POST /api/v1/hotels/prebook
     *
     * TBO Affiliate flow: PreBook locks price/validates booking before Book.
     * Returns the raw TBO PreBook response to the frontend.
     */
    @PostMapping("/prebook")
    @Operation(summary = "PreBook (TBO affiliate)", description = "Calls TBO affiliate PreBook and returns supplier response.")
    public ResponseEntity<ApiResponse<TboAffiliatePreBookResponse>> preBook(
            HttpServletRequest httpServletRequest,
            @Valid @RequestBody HotelPreBookRequest request) {

        // Auto-detect TBO for now (this endpoint is only defined for TBO affiliate flow).
        String userSessionId = null;
        Object attr = httpServletRequest.getAttribute(RequestLoggingConfig.API_ACCESS_LOG_ATTR);
        if (attr instanceof ApiAccessLog accessLog) {
            userSessionId = accessLog.getUserSessionId();
        }
        TboAffiliatePreBookResponse resp = hotelService.preBookRawTboAndRecordItinerary(
                userSessionId,
                request.getBookingCode(),
                request.getPaymentMode()
        );
        return ResponseEntity.ok(ApiResponse.success(resp));
    }

    /**
     * POST /api/v1/hotels/book
     *
     * TBO Book (HotelBE): confirms booking after successful PreBook.
     * Returns raw TBO BookResult to the frontend.
     */
    @PostMapping("/book")
    @Operation(summary = "Book (TBO HotelBE)", description = "Calls TBO HotelBE Book and returns supplier response.")
    public ResponseEntity<ApiResponse<TboBookResponse>> book(
            @Valid @RequestBody HotelBookRequest request) {

        // Map request -> TBO contract (same field names)
        TboAffiliateBookRequest tboReq = TboAffiliateBookRequest.builder()
                .bookingCode(request.getBookingCode())
                .isVoucherBooking(request.getIsVoucherBooking())
                .guestNationality(request.getGuestNationality())
                .endUserIp(request.getEndUserIp())
                .requestedBookingMode(request.getRequestedBookingMode())
                .netAmount(request.getNetAmount())
                .clientReferenceId(request.getClientReferenceId())
                .hotelRoomsDetails(request.getHotelRoomsDetails())
                .build();

        TboBookResponse resp = hotelService.bookRawTbo(tboReq);
        return ResponseEntity.ok(ApiResponse.success(resp));
    }

    /**
     * POST /api/v1/hotels/getbookingdetails
     *
     * Frontend passthrough for TBO HotelBE Getbookingdetail.
     * Requires {@code X-API-KEY} and {@code Authorization: Bearer <refreshToken>} (validated by {@link com.vivance.hotel.security.ApiKeyBearerAuthFilter}).
     */
    @PostMapping("/getbookingdetails")
    @Operation(summary = "Get booking details (TBO HotelBE)", description = "Calls TBO Getbookingdetail and returns supplier response.")
    public ResponseEntity<ApiResponse<TboGetBookingDetailResponse>> getBookingDetails(
            @Valid @RequestBody TboGetBookingDetailRequest request) {
        log.info("[HOTEL] /api/v1/hotels/getbookingdetails called (bookingId={}, traceId={})",
                request != null ? request.getBookingId() : null,
                request != null ? request.getTraceId() : null);
        TboGetBookingDetailResponse resp = hotelService.getBookingDetailRawTbo(request);
        return ResponseEntity.ok(ApiResponse.success(resp));
    }

    /**
     * GET /api/v1/hotels/countries
     *
     * <p>Returns rows from {@code tbo_hotel_countries} for dropdowns (code + name), sorted by name.
     */
    @GetMapping("/countries")
    @Operation(summary = "List hotel countries", description = "All countries from tbo_hotel_countries, ordered by name.")
    public ResponseEntity<ApiResponse<List<HotelCountryDto>>> listHotelCountries() {
        List<HotelCountryDto> countries = hotelCountryService.listCountriesOrderedByName();
        return ResponseEntity.ok(ApiResponse.success(countries));
    }

    /**
     * GET /api/v1/hotels/cities?countryCode=...
     *
     * <p>Returns rows from {@code tbo_hotel_cities} for the given country, sorted by name.
     * Use {@code code} as TBO {@code cityId} in hotel search. Pass the country's {@code code}
     * from {@code /api/v1/hotels/countries} (e.g. {@code IN}), not the numeric {@code id}.
     */
    @GetMapping("/cities")
    @Operation(summary = "List hotel cities by country",
               description = "Cities from tbo_hotel_cities filtered by country code (matches country_code), ordered by name.")
    public ResponseEntity<ApiResponse<List<HotelCityDto>>> listHotelCities(
            @RequestParam("countryCode")
            @Parameter(description = "Country code (tbo_hotel_cities.country_code), e.g. from countries API `code` field", example = "IN", required = true)
            String countryCode) {

        List<HotelCityDto> cities = hotelCityService.listCitiesByCountryCode(countryCode);
        return ResponseEntity.ok(ApiResponse.success(cities));
    }

    /**
     * GET /api/v1/hotels/{id}
     *
     * <p>For TBO: supply {@code aggregator}, {@code externalHotelId}, {@code resultIndex},
     * and {@code traceId} (all from the search response) to fetch live hotel info.
     * Falls back to DB lookup when TBO params are absent.
     */
    @GetMapping("/{id}")
    @Operation(summary = "Get hotel details",
               description = "TBO: pass aggregator=TBO, externalHotelId, resultIndex, traceId " +
                             "from the search response for live data.")
    public ResponseEntity<ApiResponse<HotelDetailDto>> getHotelDetails(
            @PathVariable @Parameter(description = "Internal hotel ID (use 0 for aggregator-only lookup)")
            Long id,

            @RequestParam(required = false)
            @Parameter(description = "Aggregator name, e.g. TBO")
            String aggregator,

            @RequestParam(required = false)
            @Parameter(description = "TBO HotelCode from search response")
            String externalHotelId,

            @RequestParam(required = false)
            @Parameter(description = "TBO ResultIndex from search response")
            String resultIndex,

            @RequestParam(required = false)
            @Parameter(description = "TBO TraceId from search response")
            String traceId) {

        HotelDetailDto detail;
        if (aggregator != null && externalHotelId != null && resultIndex != null && traceId != null) {
            // Live aggregator path — uses TBO session context
            detail = hotelService.getHotelDetails(
                    AggregatorType.valueOf(aggregator.toUpperCase()),
                    externalHotelId, resultIndex, traceId);
        } else {
            // DB / cached path
            detail = hotelService.getHotelDetails(id);
        }
        return ResponseEntity.ok(ApiResponse.success(detail));
    }

    /**
     * GET /api/v1/hotels/{id}/availability
     *
     * <p>For TBO: supply {@code aggregator}, {@code externalHotelId}, {@code resultIndex},
     * and {@code traceId} from the search response. Returns room options with TBO room codes
     * ({@code roomTypeCode}, {@code ratePlanCode}, {@code roomIndex}) that must be sent
     * back verbatim in the Block / Book request.
     */
    @GetMapping("/{id}/availability")
    @Operation(summary = "Get real-time room availability and pricing",
               description = "TBO: pass aggregator=TBO, externalHotelId, resultIndex, traceId. " +
                             "Response rooms carry roomTypeCode/ratePlanCode needed for booking.")
    public ResponseEntity<ApiResponse<List<RoomAvailabilityDto>>> getRoomAvailability(
            @PathVariable Long id,

            @RequestParam @DateTimeFormat(iso = DateTimeFormat.ISO.DATE)
            LocalDate checkIn,

            @RequestParam @DateTimeFormat(iso = DateTimeFormat.ISO.DATE)
            LocalDate checkOut,

            @RequestParam(defaultValue = "1") @Min(1)
            int guests,

            @RequestParam(required = false) String aggregator,
            @RequestParam(required = false) String externalHotelId,
            @RequestParam(required = false) String resultIndex,
            @RequestParam(required = false) String traceId) {

        List<RoomAvailabilityDto> rooms;
        if (aggregator != null && externalHotelId != null && resultIndex != null && traceId != null) {
            rooms = hotelService.getRoomAvailability(
                    AggregatorType.valueOf(aggregator.toUpperCase()),
                    externalHotelId, resultIndex, traceId,
                    checkIn, checkOut, guests);
        } else {
            rooms = hotelService.getRoomAvailability(id, checkIn, checkOut, guests);
        }
        return ResponseEntity.ok(ApiResponse.success(rooms));
    }
}
