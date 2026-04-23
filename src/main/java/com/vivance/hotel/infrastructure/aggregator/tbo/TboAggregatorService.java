package com.vivance.hotel.infrastructure.aggregator.tbo;

import com.vivance.hotel.config.AggregatorProperties;
import com.vivance.hotel.domain.enums.AggregatorType;
import com.vivance.hotel.dto.request.HotelSearchRequest;
import com.vivance.hotel.dto.response.BookingDto;
import com.vivance.hotel.dto.response.HotelDetailDto;
import com.vivance.hotel.dto.response.HotelDto;
import com.vivance.hotel.dto.response.RoomAvailabilityDto;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.vivance.hotel.exception.AggregatorException;
import com.vivance.hotel.infrastructure.aggregator.HotelAggregatorService;
import com.vivance.hotel.infrastructure.aggregator.tbo.dto.*;
import io.github.resilience4j.circuitbreaker.annotation.CircuitBreaker;
import io.github.resilience4j.retry.annotation.Retry;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.http.*;
import org.springframework.stereotype.Service;
import org.springframework.web.client.RestTemplate;

import java.math.BigDecimal;
import java.math.RoundingMode;
import java.time.LocalDate;
import java.time.temporal.ChronoUnit;
import java.util.ArrayList;
import java.util.List;
import java.util.UUID;
import java.util.stream.Collectors;

/**
 * TBO hotel aggregator — affiliate booking flow:
 * <b>Search</b> (affiliate) → <b>PreBook</b> → <b>Book</b> (HotelBE) → <b>GetBookingDetail</b> (HotelBE).
 */
@Slf4j
@Service
@RequiredArgsConstructor
public class TboAggregatorService implements HotelAggregatorService {

    private final RestTemplate tboRestTemplate;
    private final TboAuthService tboAuthService;
    private final TboApiLogger tboApiLogger;
    private final AggregatorProperties aggregatorProperties;
    private final ObjectMapper objectMapper;

    @Override
    public AggregatorType getAggregatorType() {
        return AggregatorType.TBO;
    }

    // ─── Step 2: Search ───────────────────────────────────────────────────────

    @Override
    @CircuitBreaker(name = "aggregator", fallbackMethod = "searchHotelsFallback")
    @Retry(name = "aggregator")
    public List<HotelDto> searchHotels(HotelSearchRequest request) {
        AggregatorProperties.TboConfig cfg = aggregatorProperties.getTbo();
        String hotelCodes = request.getHotelCodes();
        String cityId     = request.getCityId();

        log.info("[TBO] ========== searchHotels ENTERED: cityId={}, hotelCodes={}, checkIn={}, checkOut={} ==========",
                cityId, hotelCodes, request.getCheckIn(), request.getCheckOut());

        if ((hotelCodes == null || hotelCodes.isBlank()) && (cityId == null || cityId.isBlank())) {
            throw new AggregatorException("TBO Search requires either hotelCodes or cityId in the request.");
        }

        String token;
        if (request.getTokenId() != null && !request.getTokenId().isBlank()) {
            token = request.getTokenId();
            log.info("[TBO] Using client-provided TokenId for affiliate Search.");
        } else {
            log.info("[TBO] No TokenId provided; authenticating...");
            token = tboAuthService.getValidToken();
            log.info("[TBO] Auth token obtained successfully.");
        }

        int guestCount = request.effectiveGuests();
        int roomCount = request.getNoOfRooms() != null && request.getNoOfRooms() > 0
                ? request.getNoOfRooms()
                : (request.getPaxRooms() != null ? request.getPaxRooms().size() : 1);

        List<TboAffiliateSearchRequest.PaxRoom> paxRooms;
        if (request.getPaxRooms() != null && !request.getPaxRooms().isEmpty()) {
            paxRooms = request.getPaxRooms().stream()
                    .map(r -> TboAffiliateSearchRequest.PaxRoom.builder()
                            .adults(r.getAdults() != null ? r.getAdults() : 1)
                            .children(r.getChildren() != null ? r.getChildren() : 0)
                            .childrenAges(r.getChildrenAges())
                            .build())
                    .toList();
        } else {
            paxRooms = buildPaxRoomsForSearch(guestCount, roomCount);
        }

        boolean refundable = request.getFilters() != null && request.getFilters().getRefundable() != null
                ? request.getFilters().getRefundable()
                : false;
        int supplierNoOfRooms = request.getFilters() != null && request.getFilters().getNoOfRooms() != null
                ? request.getFilters().getNoOfRooms()
                : 0;
        String mealType = request.getFilters() != null ? request.getFilters().getMealType() : null;
        Integer starRating = null;
        if (request.getFilters() != null && request.getFilters().getStarRating() != null) {
            String sr = request.getFilters().getStarRating().trim();
            if (!sr.isEmpty()) {
                try {
                    starRating = Integer.parseInt(sr);
                } catch (NumberFormatException ignore) {
                    starRating = null;
                }
            }
        }
        if (starRating == null) {
            starRating = request.getMinStarRating();
        }

        TboAffiliateSearchRequest tboReq = TboAffiliateSearchRequest.builder()
                .checkIn(request.getCheckIn().toString())
                .checkOut(request.getCheckOut().toString())
                .hotelCodes(hotelCodes != null && !hotelCodes.isBlank() ? hotelCodes : null)
                .cityId(cityId != null && !cityId.isBlank() ? cityId : null)
                .guestNationality(request.getGuestNationality() != null ? request.getGuestNationality() : "IN")
                .paxRooms(paxRooms)
                .responseTime(request.getResponseTime() != null ? request.getResponseTime() : 23.0)
                .isDetailedResponse(request.getIsDetailedResponse() != null ? request.getIsDetailedResponse() : true)
                .tokenId(token)
                .endUserIp(request.getEndUserIp() != null && !request.getEndUserIp().isBlank()
                        ? request.getEndUserIp()
                        : cfg.getEndUserIp())
                .filters(TboAffiliateSearchRequest.Filters.builder()
                        .refundable(refundable)
                        .noOfRooms(supplierNoOfRooms)
                        // TBO expects empty string when client explicitly sends it
                        .mealType(mealType)
                        .starRating(starRating)
                        .build())
                .build();

        String searchUrl = cfg.getAffiliateSearchUrl();

        logTboSearchRequest(searchUrl, tboReq);
        TboAffiliateSearchResponse response = postAffiliate(
                "TboAffiliateSearch",
                searchUrl,
                token,
                tboReq,
                TboAffiliateSearchResponse.class
        );
        logTboSearchResponse(response);
        validateAffiliateStatus(response.getStatus(), "TboAffiliateSearch", searchUrl);

        List<TboAffiliateSearchResponse.HotelResult> hotelResults =
                response.getHotelResult() != null ? response.getHotelResult() : List.of();

        return hotelResults.stream()
                .map(this::mapAffiliateHotelToHotelDto)
                .toList();
    }

    /**
     * TBO affiliate search passthrough (raw response).
     * Used when the frontend expects TBO's native response schema.
     */
    @CircuitBreaker(name = "aggregator", fallbackMethod = "searchHotelsRawFallback")
    @Retry(name = "aggregator")
    public TboAffiliateSearchResponse searchHotelsRaw(HotelSearchRequest request) {
        // Build exactly the same outbound body as searchHotels(), but return parsed raw response.
        AggregatorProperties.TboConfig cfg = aggregatorProperties.getTbo();
        String hotelCodes = request.getHotelCodes();
        String cityId     = request.getCityId();

        if ((hotelCodes == null || hotelCodes.isBlank()) && (cityId == null || cityId.isBlank())) {
            throw new AggregatorException("TBO Search requires either hotelCodes or cityId in the request.");
        }

        String token;
        if (request.getTokenId() != null && !request.getTokenId().isBlank()) {
            token = request.getTokenId();
        } else {
            token = tboAuthService.getValidToken();
        }

        int guestCount = request.effectiveGuests();
        int roomCount = request.getNoOfRooms() != null && request.getNoOfRooms() > 0
                ? request.getNoOfRooms()
                : (request.getPaxRooms() != null ? request.getPaxRooms().size() : 1);

        List<TboAffiliateSearchRequest.PaxRoom> paxRooms;
        if (request.getPaxRooms() != null && !request.getPaxRooms().isEmpty()) {
            paxRooms = request.getPaxRooms().stream()
                    .map(r -> TboAffiliateSearchRequest.PaxRoom.builder()
                            .adults(r.getAdults() != null ? r.getAdults() : 1)
                            .children(r.getChildren() != null ? r.getChildren() : 0)
                            .childrenAges(r.getChildrenAges())
                            .build())
                    .toList();
        } else {
            paxRooms = buildPaxRoomsForSearch(guestCount, roomCount);
        }

        boolean refundable = request.getFilters() != null && request.getFilters().getRefundable() != null
                ? request.getFilters().getRefundable()
                : false;
        int supplierNoOfRooms = request.getFilters() != null && request.getFilters().getNoOfRooms() != null
                ? request.getFilters().getNoOfRooms()
                : 0;
        String mealType = request.getFilters() != null ? request.getFilters().getMealType() : null;
        Integer starRating = null;
        if (request.getFilters() != null && request.getFilters().getStarRating() != null) {
            String sr = request.getFilters().getStarRating().trim();
            if (!sr.isEmpty()) {
                try { starRating = Integer.parseInt(sr); } catch (NumberFormatException ignore) { }
            }
        }
        if (starRating == null) {
            starRating = request.getMinStarRating();
        }

        TboAffiliateSearchRequest tboReq = TboAffiliateSearchRequest.builder()
                .checkIn(request.getCheckIn().toString())
                .checkOut(request.getCheckOut().toString())
                .hotelCodes(hotelCodes != null && !hotelCodes.isBlank() ? hotelCodes : null)
                .cityId(cityId != null && !cityId.isBlank() ? cityId : null)
                .guestNationality(request.getGuestNationality() != null ? request.getGuestNationality() : "IN")
                .paxRooms(paxRooms)
                .responseTime(request.getResponseTime() != null ? request.getResponseTime() : 23.0)
                .isDetailedResponse(request.getIsDetailedResponse() != null ? request.getIsDetailedResponse() : true)
                .tokenId(token)
                .endUserIp(request.getEndUserIp() != null && !request.getEndUserIp().isBlank()
                        ? request.getEndUserIp()
                        : cfg.getEndUserIp())
                .filters(TboAffiliateSearchRequest.Filters.builder()
                        .refundable(refundable)
                        .noOfRooms(supplierNoOfRooms)
                        .mealType(mealType)
                        .starRating(starRating)
                        .build())
                .build();

        String searchUrl = cfg.getAffiliateSearchUrl();
        TboAffiliateSearchResponse response = postAffiliate(
                "TboAffiliateSearchRaw",
                searchUrl,
                token,
                tboReq,
                TboAffiliateSearchResponse.class
        );
        validateAffiliateStatus(response.getStatus(), "TboAffiliateSearchRaw", searchUrl);
        return response;
    }

    /**
     * TBO affiliate PreBook passthrough (raw response).
     */
    @CircuitBreaker(name = "aggregator", fallbackMethod = "preBookRawFallback")
    @Retry(name = "aggregator")
    public TboAffiliatePreBookResponse preBookRaw(String bookingCode, String paymentMode) {
        AggregatorProperties.TboConfig cfg = aggregatorProperties.getTbo();
        if (bookingCode == null || bookingCode.isBlank()) {
            throw new AggregatorException("BookingCode is required for TBO PreBook.");
        }
        String mode = (paymentMode == null || paymentMode.isBlank()) ? "Limit" : paymentMode;

        TboAffiliatePreBookRequest req = new TboAffiliatePreBookRequest(bookingCode, mode);
        String url = cfg.getAffiliatePreBookUrl();
        logTboPreBookRequest(url, req);
        // Affiliate API requires HTTP Basic Auth as well.
        TboAffiliatePreBookResponse resp = postAffiliate(
                "TboAffiliatePreBookRaw",
                url,
                null,
                req,
                TboAffiliatePreBookResponse.class
        );
        logTboPreBookResponse(resp);
        validateAffiliateStatus(resp.getStatus(), "TboAffiliatePreBookRaw", url);
        return resp;
    }

    /**
     * TBO Book passthrough (raw request/response).
     */
    @CircuitBreaker(name = "aggregator", fallbackMethod = "bookRawFallback")
    @Retry(name = "aggregator")
    public TboBookResponse bookRaw(TboAffiliateBookRequest request) {
        AggregatorProperties.TboConfig cfg = aggregatorProperties.getTbo();
        if (request == null || request.getBookingCode() == null || request.getBookingCode().isBlank()) {
            throw new AggregatorException("BookingCode is required for TBO Book.");
        }
        String url = cfg.getBaseUrl() + cfg.getBookPath();
        logTboBookRequest(url, request);
        // Affiliate API requires Basic Auth (same as PreBook).
        TboBookResponse resp = postAffiliate("TboBookRaw", url, null, request, TboBookResponse.class);
        logTboBookResponse(resp);
        TboBookResponse.BookResult result = resp.getBookResult();
        validateStatus(result.getResponseStatus(), result.getError(), "TboBookRaw", url);
        return resp;
    }

    @Override
    @CircuitBreaker(name = "aggregator", fallbackMethod = "getHotelDetailsFallback")
    @Retry(name = "aggregator")
    public HotelDetailDto getHotelDetails(String externalHotelId, String resultIndex, String traceId) {
        throw new AggregatorException("TBO affiliate flow does not support GetHotelInfo. Use Search + PreBook response.");
    }

    @Override
    @CircuitBreaker(name = "aggregator", fallbackMethod = "getRoomAvailabilityFallback")
    @Retry(name = "aggregator")
    public List<RoomAvailabilityDto> getRoomAvailability(
            String externalHotelId, String resultIndex, String traceId,
            LocalDate checkIn, LocalDate checkOut, int guests) {
        AggregatorProperties.TboConfig cfg = aggregatorProperties.getTbo();
        String token = tboAuthService.getValidToken();
        TboAffiliateSearchRequest tboReq = TboAffiliateSearchRequest.builder()
                .checkIn(checkIn.toString())
                .checkOut(checkOut.toString())
                .hotelCodes(externalHotelId)
                .guestNationality("IN")
                .paxRooms(List.of(TboAffiliateSearchRequest.PaxRoom.builder()
                        .adults(guests)
                        .children(0)
                        .childrenAges(null)
                        .build()))
                .responseTime(23.0)
                .isDetailedResponse(true)
                .tokenId(token)
                .endUserIp(cfg.getEndUserIp())
                .filters(TboAffiliateSearchRequest.Filters.builder()
                        .refundable(false)
                        .noOfRooms(0)
                        .mealType(null)
                        .starRating(null)
                        .build())
                .build();

        String roomUrl = cfg.getAffiliateSearchUrl();
        TboAffiliateSearchResponse response = postAffiliate("TboAffiliateSearchForRooms", roomUrl, token, tboReq, TboAffiliateSearchResponse.class);
        validateAffiliateStatus(response.getStatus(), "TboAffiliateSearchForRooms", roomUrl);

        List<TboAffiliateSearchResponse.HotelResult> hotelResults = response.getHotelResult();
        if (hotelResults == null || hotelResults.isEmpty() || hotelResults.get(0).getRooms() == null) {
            return List.of();
        }
        long nights = Math.max(1, ChronoUnit.DAYS.between(checkIn, checkOut));
        String currency = hotelResults.get(0).getCurrency() != null ? hotelResults.get(0).getCurrency() : "INR";
        return hotelResults.get(0).getRooms().stream()
                .map(r -> mapAffiliateRoomToAvailability(r, currency, nights))
                .toList();
    }

    // ─── Step 5+6: Block then Book (internal — exposed via bookHotel interface) ─

    @Override
    @CircuitBreaker(name = "aggregator", fallbackMethod = "bookHotelFallback")
    @Retry(name = "aggregator")
    public BookingDto bookHotel(
            String externalHotelId, String externalRoomId,
            LocalDate checkIn, LocalDate checkOut,
            int guests, String guestName, String guestEmail,
            String resultIndex, String traceId,
            String roomTypeCode, String ratePlanCode, String guestNationality) {
        AggregatorProperties.TboConfig cfg = aggregatorProperties.getTbo();
        String bookingCode = externalRoomId;
        if (bookingCode == null || bookingCode.isBlank()) {
            throw new AggregatorException("BookingCode is required for TBO affiliate booking flow.");
        }

        // ── Step 2: PreBook ────────────────────────────────────────────────
        TboAffiliatePreBookRequest preBookReq = new TboAffiliatePreBookRequest(bookingCode, "Limit");
        String preBookUrl = cfg.getAffiliatePreBookUrl();
        // Affiliate API requires Basic Auth.
        TboAffiliatePreBookResponse preBookResp = postAffiliate(
                "TboAffiliatePreBook",
                preBookUrl,
                null,
                preBookReq,
                TboAffiliatePreBookResponse.class
        );
        validateAffiliateStatus(preBookResp.getStatus(), "TboAffiliatePreBook", preBookUrl);

        BigDecimal netAmount = extractNetAmountFromPreBook(preBookResp);
        String[] nameParts = splitName(guestName);

        TboAffiliateBookRequest bookReq = TboAffiliateBookRequest.builder()
                .bookingCode(bookingCode)
                .isVoucherBooking(true)
                .guestNationality(guestNationality != null ? guestNationality : "IN")
                .endUserIp(cfg.getEndUserIp())
                .requestedBookingMode(5)
                .netAmount(netAmount)
                .clientReferenceId("VH-" + UUID.randomUUID())
                .hotelRoomsDetails(List.of(
                        TboAffiliateBookRequest.HotelRoomDetail.builder()
                                .hotelPassenger(List.of(
                                        TboAffiliateBookRequest.Passenger.builder()
                                                .title("Mr.")
                                                .firstName(nameParts[0])
                                                .lastName(nameParts[1])
                                                .email(guestEmail != null && !guestEmail.isBlank() ? guestEmail : null)
                                                .paxType(1)
                                                .leadPassenger(true)
                                                .age(0)
                                                .paxId(0)
                                                .build()
                                ))
                                .build()
                ))
                .build();

        String bookUrl = cfg.getBaseUrl() + cfg.getBookPath();
        // Affiliate API requires Basic Auth (same as PreBook).
        TboBookResponse bookResp = postAffiliate("TboBook", bookUrl, null, bookReq, TboBookResponse.class);

        TboBookResponse.BookResult bookResult = bookResp.getBookResult();
        validateStatus(bookResult.getResponseStatus(), bookResult.getError(), "TboBook", bookUrl);

        // ── Step 4: GetBookingDetail ───────────────────────────────────────
        if (bookResult.getBookingId() > 0) {
            getBookingDetail(String.valueOf(bookResult.getBookingId()), null);
        } else if (bookResult.getTraceId() != null && !bookResult.getTraceId().isBlank()) {
            getBookingDetail(null, bookResult.getTraceId());
        }

        log.info("[TBO] Booking confirmed: status={}, confirmationNo={}, bookingRefNo={}",
                bookResult.getHotelBookingStatus(), bookResult.getConfirmationNo(),
                bookResult.getBookingRefNo());

        return BookingDto.builder()
                .aggregatorBookingId(bookResult.getBookingRefNo())
                .build();
    }

    // ─── Step 4 (post-book): GetBookingDetail ─────────────────────────────────

    public TboGetBookingDetailResponse.GetBookingDetailResult getBookingDetail(String bookingId) {
        return getBookingDetail(bookingId, null);
    }

    /**
     * Loads booking state from HotelBE using either numeric {@code BookingId} or {@code TraceId}.
     */
    public TboGetBookingDetailResponse.GetBookingDetailResult getBookingDetail(
            String bookingId, String traceId) {
        if ((bookingId == null || bookingId.isBlank()) && (traceId == null || traceId.isBlank())) {
            throw new AggregatorException("TBO GetBookingDetail requires bookingId or traceId.");
        }
        String token = tboAuthService.getValidToken();
        AggregatorProperties.TboConfig cfg = aggregatorProperties.getTbo();

        TboGetBookingDetailRequest req = TboGetBookingDetailRequest.builder()
                .bookingId(bookingId != null && !bookingId.isBlank() ? bookingId : null)
                .traceId(traceId != null && !traceId.isBlank() ? traceId : null)
                .endUserIp(cfg.getEndUserIp())
                .tokenId(token)
                .build();

        String detailUrl = cfg.getBaseUrl() + cfg.getBookingDetailPath();
        log.info("[TBO] GetBookingDetail: bookingId={}, traceId={}", bookingId, traceId);
        TboGetBookingDetailResponse response = post(
                "TboGetBookingDetail",
                detailUrl,
                token,
                req,
                TboGetBookingDetailResponse.class
        );

        TboGetBookingDetailResponse.GetBookingDetailResult result =
                response.getGetBookingDetailResult();
        validateStatus(result.getResponseStatus(), result.getError(), "TboGetBookingDetail", detailUrl);

        return result;
    }

    // ─── Console logging helpers ──────────────────────────────────────────────

    private void logTboSearchRequest(String url, TboAffiliateSearchRequest req) {
        try {
            String json = objectMapper.writerWithDefaultPrettyPrinter().writeValueAsString(req);
            log.info("\n" +
                    "╔══════════════════════════════════════════════════════════════╗\n" +
                    "║              TBO HOTEL SEARCH — OUTGOING REQUEST            ║\n" +
                    "╠══════════════════════════════════════════════════════════════╣\n" +
                    "║ Method : POST                                                ║\n" +
                    "║ URL    : {}  \n" +
                    "╠══════════════════════════════════════════════════════════════╣\n" +
                    "  Request Body:\n{}\n" +
                    "╚══════════════════════════════════════════════════════════════╝",
                    url, json);
        } catch (Exception e) {
            log.warn("[TBO] Could not serialize search request for logging: {}", e.getMessage());
        }
    }

    private void logTboSearchResponse(TboAffiliateSearchResponse response) {
        try {
            int hotelCount = response.getHotelResult() != null ? response.getHotelResult().size() : 0;
            int statusCode = response.getStatus() != null && response.getStatus().getCode() != null
                    ? response.getStatus().getCode() : -1;
            String json = objectMapper.writerWithDefaultPrettyPrinter().writeValueAsString(response);
            log.info("\n" +
                    "╔══════════════════════════════════════════════════════════════╗\n" +
                    "║              TBO HOTEL SEARCH — INCOMING RESPONSE           ║\n" +
                    "╠══════════════════════════════════════════════════════════════╣\n" +
                    "║ Status : {}  Hotels Found : {}                               \n" +
                    "╠══════════════════════════════════════════════════════════════╣\n" +
                    "  Response Body:\n{}\n" +
                    "╚══════════════════════════════════════════════════════════════╝",
                    statusCode, hotelCount, json);
        } catch (Exception e) {
            log.warn("[TBO] Could not serialize search response for logging: {}", e.getMessage());
        }
    }

    private void logTboPreBookRequest(String url, TboAffiliatePreBookRequest req) {
        try {
            String json = objectMapper.writerWithDefaultPrettyPrinter().writeValueAsString(req);
            log.info("\n" +
                            "╔══════════════════════════════════════════════════════════════╗\n" +
                            "║              TBO PREBOOK — OUTGOING REQUEST                 ║\n" +
                            "╠══════════════════════════════════════════════════════════════╣\n" +
                            "║ Method : POST                                                ║\n" +
                            "║ URL    : {}  \n" +
                            "╠══════════════════════════════════════════════════════════════╣\n" +
                            "  Request Body:\n{}\n" +
                            "╚══════════════════════════════════════════════════════════════╝",
                    url, json);
        } catch (Exception e) {
            log.warn("[TBO] Could not serialize prebook request for logging: {}", e.getMessage());
        }
    }

    private void logTboPreBookResponse(TboAffiliatePreBookResponse response) {
        try {
            int statusCode = response.getStatus() != null && response.getStatus().getCode() != null
                    ? response.getStatus().getCode() : -1;
            String json = objectMapper.writerWithDefaultPrettyPrinter().writeValueAsString(response);
            log.info("\n" +
                            "╔══════════════════════════════════════════════════════════════╗\n" +
                            "║              TBO PREBOOK — INCOMING RESPONSE                ║\n" +
                            "╠══════════════════════════════════════════════════════════════╣\n" +
                            "║ Status : {}                                                  \n" +
                            "╠══════════════════════════════════════════════════════════════╣\n" +
                            "  Response Body:\n{}\n" +
                            "╚══════════════════════════════════════════════════════════════╝",
                    statusCode, json);
        } catch (Exception e) {
            log.warn("[TBO] Could not serialize prebook response for logging: {}", e.getMessage());
        }
    }

    private void logTboBookRequest(String url, TboAffiliateBookRequest req) {
        try {
            String json = objectMapper.writerWithDefaultPrettyPrinter().writeValueAsString(req);
            log.info("\n" +
                            "╔══════════════════════════════════════════════════════════════╗\n" +
                            "║              TBO BOOK — OUTGOING REQUEST                    ║\n" +
                            "╠══════════════════════════════════════════════════════════════╣\n" +
                            "║ Method : POST                                                ║\n" +
                            "║ URL    : {}  \n" +
                            "╠══════════════════════════════════════════════════════════════╣\n" +
                            "  Request Body:\n{}\n" +
                            "╚══════════════════════════════════════════════════════════════╝",
                    url, json);
        } catch (Exception e) {
            log.warn("[TBO] Could not serialize book request for logging: {}", e.getMessage());
        }
    }

    private void logTboBookResponse(TboBookResponse response) {
        try {
            int rs = response.getBookResult() != null ? response.getBookResult().getResponseStatus() : -1;
            String json = objectMapper.writerWithDefaultPrettyPrinter().writeValueAsString(response);
            log.info("\n" +
                            "╔══════════════════════════════════════════════════════════════╗\n" +
                            "║              TBO BOOK — INCOMING RESPONSE                   ║\n" +
                            "╠══════════════════════════════════════════════════════════════╣\n" +
                            "║ ResponseStatus : {}                                          \n" +
                            "╠══════════════════════════════════════════════════════════════╣\n" +
                            "  Response Body:\n{}\n" +
                            "╚══════════════════════════════════════════════════════════════╝",
                    rs, json);
        } catch (Exception e) {
            log.warn("[TBO] Could not serialize book response for logging: {}", e.getMessage());
        }
    }

    // ─── Fallback Methods ─────────────────────────────────────────────────────

    public List<HotelDto> searchHotelsFallback(HotelSearchRequest req, Throwable t) {
        log.error("[TBO] Circuit breaker — searchHotels: {}", t.getMessage());
        throw new AggregatorException("TBO hotel search is currently unavailable. Please try again later.");
    }

    public TboAffiliateSearchResponse searchHotelsRawFallback(HotelSearchRequest req, Throwable t) {
        log.error("[TBO] Circuit breaker — searchHotelsRaw: {}", t.getMessage());
        throw new AggregatorException("TBO hotel search is currently unavailable. Please try again later.");
    }

    public TboAffiliatePreBookResponse preBookRawFallback(String bookingCode, String paymentMode, Throwable t) {
        log.error("[TBO] Circuit breaker — preBookRaw: {}", t.getMessage());
        throw new AggregatorException("TBO prebook service is currently unavailable. Please try again later.");
    }

    public TboBookResponse bookRawFallback(TboAffiliateBookRequest request, Throwable t) {
        log.error("[TBO] Circuit breaker — bookRaw: {}", t.getMessage());
        throw new AggregatorException("TBO booking service is currently unavailable. Please try again later.");
    }

    public HotelDetailDto getHotelDetailsFallback(String id, String idx, String trace, Throwable t) {
        log.error("[TBO] Circuit breaker — getHotelDetails: {}", t.getMessage());
        throw new AggregatorException("TBO hotel details are currently unavailable. Please try again later.");
    }

    public List<RoomAvailabilityDto> getRoomAvailabilityFallback(
            String id, String idx, String trace, LocalDate ci, LocalDate co, int g, Throwable t) {
        log.error("[TBO] Circuit breaker — getRoomAvailability: {}", t.getMessage());
        throw new AggregatorException("TBO room availability is currently unavailable. Please try again later.");
    }

    public BookingDto bookHotelFallback(
            String hotelId, String roomId, LocalDate ci, LocalDate co, int g,
            String name, String email, String idx, String trace,
            String rtc, String rpc, String nat, Throwable t) {
        log.error("[TBO] Circuit breaker — bookHotel: {}", t.getMessage());
        throw new AggregatorException("TBO booking service is currently unavailable. Please try again later.");
    }

    // ─── Mapping helpers ──────────────────────────────────────────────────────

    private HotelDto mapAffiliateHotelToHotelDto(TboAffiliateSearchResponse.HotelResult h) {
        BigDecimal minFare = h.getRooms() == null ? null : h.getRooms().stream()
                .map(TboAffiliateSearchResponse.Room::getTotalFare)
                .filter(v -> v != null)
                .min(BigDecimal::compareTo)
                .orElse(null);

        return HotelDto.builder()
                .name("TBO Hotel " + h.getHotelCode())
                .city("")
                .country("")
                .address("")
                .starRating(0)
                .imageUrls(List.of())
                .amenities(List.of())
                .startingPrice(minFare)
                .currency(h.getCurrency() != null ? h.getCurrency() : "INR")
                .aggregatorSource("TBO")
                .externalHotelId(h.getHotelCode())
                .resultIndex(null)
                .traceId(null)
                .build();
    }

    private static List<TboAffiliateSearchRequest.PaxRoom> buildPaxRoomsForSearch(int guests, int roomCount) {
        int n = Math.max(1, roomCount);
        int g = Math.max(1, guests);
        int base = g / n;
        int rem = g % n;
        List<TboAffiliateSearchRequest.PaxRoom> list = new ArrayList<>(n);
        for (int i = 0; i < n; i++) {
            int adults = base + (i < rem ? 1 : 0);
            if (adults < 1) {
                adults = 1;
            }
            list.add(TboAffiliateSearchRequest.PaxRoom.builder()
                    .adults(adults)
                    .children(0)
                    .childrenAges(null)
                    .build());
        }
        return list;
    }

    private RoomAvailabilityDto mapAffiliateRoomToAvailability(
            TboAffiliateSearchResponse.Room r, String currency, long nights) {
        String roomType;
        if (r.getName() == null || r.getName().isEmpty()) {
            roomType = "Room";
        } else {
            roomType = r.getName().stream().distinct().collect(Collectors.joining(" + "));
        }
        String promo = (r.getRoomPromotion() != null && !r.getRoomPromotion().isEmpty())
                ? String.join("; ", r.getRoomPromotion()) : null;
        BigDecimal total = r.getTotalFare() != null ? r.getTotalFare() : BigDecimal.ZERO;
        BigDecimal perNight = nights > 0
                ? total.divide(BigDecimal.valueOf(nights), 2, RoundingMode.HALF_UP)
                : total;

        return RoomAvailabilityDto.builder()
                .roomType(roomType)
                .pricePerNight(perNight)
                .totalPrice(total)
                .currency(currency != null ? currency : "INR")
                .available(true)
                .description(r.getInclusion())
                .amenities(r.getAmenities() != null ? r.getAmenities() : List.of())
                .roomIndex(r.getBookingCode())
                .roomTypeCode(roomType)
                .roomTypeName(roomType)
                .ratePlanCode(r.getMealType())
                .availabilityType(Boolean.TRUE.equals(r.getIsRefundable()) ? "Refundable" : "NonRefundable")
                .cancellationPolicy(formatAffiliateCancelPolicies(r.getCancelPolicies()))
                .lastCancellationDate(null)
                .isPANMandatory(false)
                .isPassportMandatory(false)
                .roomPromotion(promo)
                .tboRoomIds(r.getRoomId())
                .build();
    }

    private static String formatAffiliateCancelPolicies(List<TboAffiliateSearchResponse.CancelPolicy> policies) {
        if (policies == null || policies.isEmpty()) {
            return null;
        }
        return policies.stream()
                .map(p -> (p.getChargeType() != null ? p.getChargeType() : "")
                        + " " + (p.getCancellationCharge() != null ? p.getCancellationCharge() : "")
                        + " from " + (p.getFromDate() != null ? p.getFromDate() : ""))
                .collect(Collectors.joining(" | "));
    }

    private void validateAffiliateStatus(TboAffiliateStatus status, String operation, String url) {
        if (status == null || status.getCode() == null || status.getCode() != 200) {
            String msg = status != null ? status.getDescription() : "Unknown affiliate status";
            tboApiLogger.logError(operation, url, null, msg);
            throw new AggregatorException("[TBO] " + operation + " failed: " + msg);
        }
    }

    private BigDecimal extractNetAmountFromPreBook(TboAffiliatePreBookResponse preBookResp) {
        if (preBookResp.getHotelResult() == null || preBookResp.getHotelResult().isEmpty()) {
            return BigDecimal.ZERO;
        }
        TboAffiliateSearchResponse.HotelResult hotel = preBookResp.getHotelResult().get(0);
        if (hotel.getRooms() == null || hotel.getRooms().isEmpty()) {
            return BigDecimal.ZERO;
        }
        TboAffiliateSearchResponse.Room room = hotel.getRooms().get(0);
        BigDecimal fare = room.getTotalFare();
        if (fare == null) {
            return BigDecimal.ZERO;
        }
        BigDecimal tax = room.getTotalTax();
        return tax != null ? fare.add(tax) : fare;
    }

    // ─── HTTP helpers ─────────────────────────────────────────────────────────

    private <T> T post(String url, Object requestBody, Class<T> responseType) {
        return post(null, url, null, requestBody, responseType);
    }

    private <T> T post(String operation, String url, String tokenId, Object requestBody, Class<T> responseType) {
        HttpHeaders headers = new HttpHeaders();
        headers.setContentType(MediaType.APPLICATION_JSON);
        headers.setAccept(List.of(MediaType.ALL));
        return postInternal(operation, url, tokenId, headers, requestBody, responseType);
    }

    /**
     * Like {@link #post} but adds HTTP Basic Auth (TBO affiliate API requires this
     * in addition to the TokenId in the request body).
     */
    private <T> T postAffiliate(String operation, String url, String tokenId, Object requestBody, Class<T> responseType) {
        AggregatorProperties.TboConfig cfg = aggregatorProperties.getTbo();
        HttpHeaders headers = new HttpHeaders();
        headers.setContentType(MediaType.APPLICATION_JSON);
        headers.setAccept(List.of(MediaType.ALL));
        headers.setBasicAuth(cfg.getUserName(), cfg.getPassword());
        return postInternal(operation, url, tokenId, headers, requestBody, responseType);
    }

    /**
     * Core HTTP POST: always reads the raw response body as a String first to detect
     * HTML error pages before Jackson attempts deserialization.
     */
    private <T> T postInternal(String operation, String url, String tokenId,
                                HttpHeaders headers, Object requestBody, Class<T> responseType) {
        HttpEntity<Object> entity = new HttpEntity<>(requestBody, headers);
        if (operation != null) {
            tboApiLogger.logRequest(operation, url, tokenId, headers, requestBody);
        }

        ResponseEntity<String> raw = tboRestTemplate.exchange(
                url, HttpMethod.POST, entity, String.class);

        String body = raw.getBody();
        MediaType contentType = raw.getHeaders().getContentType();

        if (body == null || body.isBlank()) {
            throw new AggregatorException("Empty response from TBO at: " + url);
        }

        // TBO sometimes returns an HTML error page instead of JSON
        if (contentType != null && contentType.isCompatibleWith(MediaType.TEXT_HTML)) {
            String preview = body.length() > 3000 ? body.substring(0, 3000) + "\n... [TRUNCATED]" : body;
            log.error("[TBO] Received HTML instead of JSON from {}.\nContent-Type: {}\nBody:\n{}",
                    url, contentType, preview);
            throw new AggregatorException(
                    "TBO returned an HTML error page instead of JSON from: " + url +
                    ". Check logs for the HTML content — the token may be invalid or the endpoint path may have changed.");
        }

        if (operation != null) {
            tboApiLogger.logRawJsonResponse(operation, url, tokenId,
                    raw.getStatusCode().value(), raw.getHeaders(), body);
        }

        try {
            return objectMapper.readValue(body, responseType);
        } catch (Exception e) {
            String preview = body.length() > 800 ? body.substring(0, 800) + "..." : body;
            log.error("[TBO] Failed to parse JSON from {}.\nBody:\n{}", url, preview);
            throw new AggregatorException("Failed to parse TBO response from " + url + ": " + e.getMessage());
        }
    }

    private void validateStatus(int responseStatus, TboError error, String operation, String url) {
        if (responseStatus != 1) {
            String msg = error != null && error.getErrorMessage() != null
                    ? error.getErrorMessage()
                    : "Unknown error (code " + (error != null ? error.getErrorCode() : "?") + ")";
            tboApiLogger.logError(operation, url, null, msg);
            throw new AggregatorException("[TBO] " + operation + " failed: " + msg);
        }
        if (error != null && error.hasError()) {
            tboApiLogger.logError(operation, url, null, error.getErrorMessage());
            throw new AggregatorException("[TBO] " + operation + " error: " + error.getErrorMessage());
        }
    }

    /** Splits "First Last" → ["First", "Last"]. Handles single-name edge case. */
    private String[] splitName(String fullName) {
        if (fullName == null || fullName.isBlank()) return new String[]{"Guest", "User"};
        int space = fullName.lastIndexOf(' ');
        if (space < 0) return new String[]{fullName, "."};
        return new String[]{fullName.substring(0, space), fullName.substring(space + 1)};
    }
}
