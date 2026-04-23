package com.vivance.hotel.controller;

import com.vivance.hotel.domain.enums.AggregatorType;
import com.vivance.hotel.dto.request.HotelSearchRequest;
import com.vivance.hotel.dto.request.HotelPreBookRequest;
import com.vivance.hotel.dto.request.HotelBookRequest;
import com.vivance.hotel.dto.response.ApiResponse;
import com.vivance.hotel.dto.response.HotelDetailDto;
import com.vivance.hotel.dto.response.HotelDto;
import com.vivance.hotel.dto.response.RoomAvailabilityDto;
import com.vivance.hotel.infrastructure.aggregator.tbo.dto.TboAffiliatePreBookResponse;
import com.vivance.hotel.infrastructure.aggregator.tbo.dto.TboAffiliateBookRequest;
import com.vivance.hotel.infrastructure.aggregator.tbo.dto.TboAffiliateSearchResponse;
import com.vivance.hotel.infrastructure.aggregator.tbo.dto.TboBookResponse;
import com.vivance.hotel.infrastructure.aggregator.tbo.dto.TboGetBookingDetailRequest;
import com.vivance.hotel.infrastructure.aggregator.tbo.dto.TboGetBookingDetailResponse;
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

import java.time.LocalDate;
import java.util.List;

@Slf4j
@Validated
@RestController
@RequestMapping("/api/v1/hotels")
@RequiredArgsConstructor
@Tag(name = "Hotels", description = "Hotel search, details, and room availability")
public class HotelController {

    private final HotelService hotelService;

    /**
     * POST /api/v1/hotels/search
     *
     * <p>For TBO, {@code cityId} (TBO's numeric city code) and {@code countryCode} are required.
     * The response includes {@code traceId} and {@code resultIndex} per hotel — store these
     * and pass them in hotel-detail and availability calls.
     */
    @PostMapping("/search")
    @Operation(summary = "Search hotels by city, dates, and guest count",
               description = "TBO: supply cityId (numeric TBO city code). Response includes " +
                             "traceId and resultIndex per hotel — required for all follow-up calls.")
    public ResponseEntity<ApiResponse<?>> searchHotels(
            @Valid @RequestBody HotelSearchRequest request) {

        log.debug("Hotel search: city={}, cityId={}, checkIn={}, checkOut={}, guests={}",
                request.getCity(), request.getCityId(),
                request.getCheckIn(), request.getCheckOut(), request.getGuests());

        // If frontend is using TBO affiliate schema, return the raw TBO response.
        // Auto-detect TBO affiliate search when HotelCodes/CityId are provided (even if Aggregator is omitted).
        boolean isTboRawSearch =
                (request.getAggregator() != null && "TBO".equalsIgnoreCase(request.getAggregator()))
                        || ((request.getHotelCodes() != null && !request.getHotelCodes().isBlank())
                        || (request.getCityId() != null && !request.getCityId().isBlank()));

        if (isTboRawSearch) {
            TboAffiliateSearchResponse raw = hotelService.searchHotelsRawTbo(request);
            return ResponseEntity.ok(ApiResponse.success(raw));
        }

        List<HotelDto> results = hotelService.searchHotels(request);
        return ResponseEntity.ok(ApiResponse.success("Found " + results.size() + " hotels", results));
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
            @Valid @RequestBody HotelPreBookRequest request) {

        // Auto-detect TBO for now (this endpoint is only defined for TBO affiliate flow).
        TboAffiliatePreBookResponse resp = hotelService.preBookRawTbo(
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
        TboGetBookingDetailResponse resp = hotelService.getBookingDetailRawTbo(request);
        return ResponseEntity.ok(ApiResponse.success(resp));
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
