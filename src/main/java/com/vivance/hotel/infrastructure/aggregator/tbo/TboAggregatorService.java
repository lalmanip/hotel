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
import java.time.LocalDate;
import java.time.format.DateTimeFormatter;
import java.time.temporal.ChronoUnit;
import java.util.List;
import java.util.UUID;

/**
 * TBO hotel aggregator — implements all 6 TBO API steps:
 * Authenticate → Search → GetHotelInfo → GetHotelRoom → Block → Book
 *
 * <p>Authentication is managed by {@link TboAuthService}: one token per calendar day
 * (IST), persisted in DB, shared with the Flight microservice.
 *
 * <p>{@code TraceId} from the search response is embedded in {@link HotelDto#getTraceId()}
 * and must flow back through hotel-detail, room-availability, and booking calls.
 */
@Slf4j
@Service
@RequiredArgsConstructor
public class TboAggregatorService implements HotelAggregatorService {

    private static final DateTimeFormatter TBO_DATE_FMT = DateTimeFormatter.ofPattern("dd/MM/yyyy");

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
        String token = tboAuthService.getValidToken();
        AggregatorProperties.TboConfig cfg = aggregatorProperties.getTbo();

        long nights = ChronoUnit.DAYS.between(request.getCheckIn(), request.getCheckOut());
        int rooms = request.getNoOfRooms() != null ? request.getNoOfRooms() : 1;

        TboHotelSearchRequest tboReq = TboHotelSearchRequest.builder()
                .checkInDate(request.getCheckIn().format(TBO_DATE_FMT))
                .noOfNights(String.valueOf(nights))
                .countryCode(request.getCountryCode() != null ? request.getCountryCode() : "IN")
                .cityId(request.getCityId())
                .preferredCurrency("INR")
                .guestNationality(request.getGuestNationality() != null ? request.getGuestNationality() : "IN")
                .noOfRooms(String.valueOf(rooms))
                .roomGuests(List.of(TboRoomGuest.builder()
                        .noOfAdults(request.getGuests())
                        .noOfChild(0)
                        .build()))
                .maxRating(5)
                .minRating(request.getMinStarRating() != null ? request.getMinStarRating() : 0)
                .isNearBySearchAllowed(false)
                .endUserIp(cfg.getEndUserIp())
                .tokenId(token)
                .build();

        String searchUrl = cfg.getBaseUrl() + cfg.getSearchPath();
        log.info("[TBO] Searching hotels: city={}, checkIn={}, nights={}, guests={}",
                request.getCityId(), request.getCheckIn(), nights, request.getGuests());
        tboApiLogger.logRequest("TboHotelSearch", searchUrl, token, tboReq);

        TboHotelSearchResponse response = post(searchUrl, tboReq, TboHotelSearchResponse.class);
        tboApiLogger.logResponse("TboHotelSearch", searchUrl, token, response);

        TboHotelSearchResponse.HotelSearchResult result = response.getHotelSearchResult();
        validateStatus(result.getResponseStatus(), result.getError(), "TboHotelSearch", searchUrl);

        String traceId = result.getTraceId();
        log.info("[TBO] Search returned {} hotels, traceId={}",
                result.getHotelResults() != null ? result.getHotelResults().size() : 0, traceId);

        return result.getHotelResults().stream()
                .map(h -> mapToHotelDto(h, traceId))
                .toList();
    }

    // ─── Step 3: GetHotelInfo ─────────────────────────────────────────────────

    @Override
    @CircuitBreaker(name = "aggregator", fallbackMethod = "getHotelDetailsFallback")
    @Retry(name = "aggregator")
    public HotelDetailDto getHotelDetails(String externalHotelId, String resultIndex, String traceId) {
        String token = tboAuthService.getValidToken();
        AggregatorProperties.TboConfig cfg = aggregatorProperties.getTbo();

        TboHotelInfoRequest tboReq = TboHotelInfoRequest.builder()
                .resultIndex(resultIndex)
                .hotelCode(externalHotelId)
                .endUserIp(cfg.getEndUserIp())
                .tokenId(token)
                .traceId(traceId)
                .build();

        String infoUrl = cfg.getBaseUrl() + cfg.getHotelInfoPath();
        log.info("[TBO] GetHotelInfo: hotelCode={}, resultIndex={}", externalHotelId, resultIndex);
        tboApiLogger.logRequest("TboGetHotelInfo", infoUrl, token, tboReq);

        TboHotelInfoResponse response = post(infoUrl, tboReq, TboHotelInfoResponse.class);
        tboApiLogger.logResponse("TboGetHotelInfo", infoUrl, token, response);

        TboHotelInfoResponse.HotelInfoResult result = response.getHotelInfoResult();
        validateStatus(result.getResponseStatus(), result.getError(), "TboGetHotelInfo", infoUrl);

        return mapToHotelDetailDto(result.getHotelDetails());
    }

    // ─── Step 4: GetHotelRoom ─────────────────────────────────────────────────

    @Override
    @CircuitBreaker(name = "aggregator", fallbackMethod = "getRoomAvailabilityFallback")
    @Retry(name = "aggregator")
    public List<RoomAvailabilityDto> getRoomAvailability(
            String externalHotelId, String resultIndex, String traceId,
            LocalDate checkIn, LocalDate checkOut, int guests) {

        String token = tboAuthService.getValidToken();
        AggregatorProperties.TboConfig cfg = aggregatorProperties.getTbo();
        long nights = ChronoUnit.DAYS.between(checkIn, checkOut);

        TboGetRoomRequest tboReq = TboGetRoomRequest.builder()
                .resultIndex(resultIndex)
                .hotelCode(externalHotelId)
                .endUserIp(cfg.getEndUserIp())
                .tokenId(token)
                .traceId(traceId)
                .build();

        String roomUrl = cfg.getBaseUrl() + cfg.getGetRoomPath();
        log.info("[TBO] GetHotelRoom: hotelCode={}, resultIndex={}, traceId={}",
                externalHotelId, resultIndex, traceId);
        tboApiLogger.logRequest("TboGetHotelRoom", roomUrl, token, tboReq);

        TboGetRoomResponse response = post(roomUrl, tboReq, TboGetRoomResponse.class);
        tboApiLogger.logResponse("TboGetHotelRoom", roomUrl, token, response);

        TboGetRoomResponse.GetHotelRoomResult result = response.getGetHotelRoomResult();
        validateStatus(result.getResponseStatus(), result.getError(), "TboGetHotelRoom", roomUrl);

        return result.getHotelRoomsDetails().stream()
                .map(r -> mapToRoomAvailabilityDto(r, nights))
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

        String token = tboAuthService.getValidToken();
        AggregatorProperties.TboConfig cfg = aggregatorProperties.getTbo();
        String[] nameParts = splitName(guestName);

        TboPrice placeholderPrice = new TboPrice();
        placeholderPrice.setCurrencyCode("INR");
        placeholderPrice.setRoomPrice(BigDecimal.ZERO);  // Block validates and corrects the price

        TboRoomDetail roomDetail = TboRoomDetail.builder()
                .roomIndex(externalRoomId)
                .roomTypeCode(roomTypeCode)
                .roomTypeName("")
                .ratePlanCode(ratePlanCode)
                .smokingPreference(0)
                .price(placeholderPrice)
                .build();

        // ── Step 5: Block ─────────────────────────────────────────────────
        TboBlockRoomRequest blockReq = TboBlockRoomRequest.builder()
                .resultIndex(resultIndex)
                .hotelCode(externalHotelId)
                .hotelName("")
                .guestNationality(guestNationality != null ? guestNationality : "IN")
                .noOfRooms("1")
                .clientReferenceNo("0")
                .isVoucherBooking("false")
                .hotelRoomsDetails(List.of(roomDetail))
                .endUserIp(cfg.getEndUserIp())
                .tokenId(token)
                .traceId(traceId)
                .build();

        String blockUrl = cfg.getBaseUrl() + cfg.getBlockPath();
        log.info("[TBO] BlockRoom: hotelCode={}, resultIndex={}", externalHotelId, resultIndex);
        tboApiLogger.logRequest("TboBlockRoom", blockUrl, token, blockReq);

        TboBlockRoomResponse blockResp = post(blockUrl, blockReq, TboBlockRoomResponse.class);
        tboApiLogger.logResponse("TboBlockRoom", blockUrl, token, blockResp);

        TboBlockRoomResponse.BlockRoomResult blockResult = blockResp.getBlockRoomResult();
        validateStatus(blockResult.getResponseStatus(), blockResult.getError(), "TboBlockRoom", blockUrl);

        if (blockResult.isPriceChanged()) {
            log.warn("[TBO] Price changed during block for hotelCode={}", externalHotelId);
        }

        // Get confirmed price from block response
        TboPrice confirmedPrice = blockResult.getHotelRoomsDetails().isEmpty()
                ? placeholderPrice
                : blockResult.getHotelRoomsDetails().get(0).getPrice();

        // ── Step 6: Book ─────────────────────────────────────────────────
        TboHotelPassenger passenger = TboHotelPassenger.builder()
                .title("Mr")
                .firstName(nameParts[0])
                .lastName(nameParts[1])
                .email(guestEmail)
                .paxType(1)
                .leadPassenger(true)
                .build();

        TboRoomDetail bookRoomDetail = TboRoomDetail.builder()
                .roomIndex(externalRoomId)
                .roomTypeCode(roomTypeCode)
                .roomTypeName("")
                .ratePlanCode(ratePlanCode)
                .smokingPreference(0)
                .price(confirmedPrice)
                .hotelPassenger(List.of(passenger))
                .build();

        TboBookRequest bookReq = TboBookRequest.builder()
                .resultIndex(resultIndex)
                .hotelCode(externalHotelId)
                .hotelName("")
                .guestNationality(guestNationality != null ? guestNationality : "IN")
                .noOfRooms("1")
                .clientReferenceNo("0")
                .isVoucherBooking("false")
                .hotelRoomsDetails(List.of(bookRoomDetail))
                .endUserIp(cfg.getEndUserIp())
                .tokenId(token)
                .traceId(traceId)
                .build();

        String bookUrl = cfg.getBaseUrl() + cfg.getBookPath();
        log.info("[TBO] BookHotel: hotelCode={}, resultIndex={}", externalHotelId, resultIndex);
        tboApiLogger.logRequest("TboBook", bookUrl, token, bookReq);

        TboBookResponse bookResp = post(bookUrl, bookReq, TboBookResponse.class);
        tboApiLogger.logResponse("TboBook", bookUrl, token, bookResp);

        TboBookResponse.BookResult bookResult = bookResp.getBookResult();
        validateStatus(bookResult.getResponseStatus(), bookResult.getError(), "TboBook", bookUrl);

        log.info("[TBO] Booking confirmed: status={}, confirmationNo={}, bookingRefNo={}",
                bookResult.getHotelBookingStatus(), bookResult.getConfirmationNo(),
                bookResult.getBookingRefNo());

        return BookingDto.builder()
                .aggregatorBookingId(bookResult.getBookingRefNo())
                .build();
    }

    // ─── Step 7: GetBookingDetail ─────────────────────────────────────────────

    public TboGetBookingDetailResponse.GetBookingDetailResult getBookingDetail(String bookingId) {
        String token = tboAuthService.getValidToken();
        AggregatorProperties.TboConfig cfg = aggregatorProperties.getTbo();

        TboGetBookingDetailRequest req = TboGetBookingDetailRequest.builder()
                .bookingId(bookingId)
                .endUserIp(cfg.getEndUserIp())
                .tokenId(token)
                .build();

        String detailUrl = cfg.getInternalBaseUrl() + cfg.getBookingDetailPath();
        log.info("[TBO] GetBookingDetail: bookingId={}", bookingId);
        tboApiLogger.logRequest("TboGetBookingDetail", detailUrl, token, req);

        TboGetBookingDetailResponse response = post(detailUrl, req, TboGetBookingDetailResponse.class);
        tboApiLogger.logResponse("TboGetBookingDetail", detailUrl, token, response);

        TboGetBookingDetailResponse.GetBookingDetailResult result =
                response.getGetBookingDetailResult();
        validateStatus(result.getResponseStatus(), result.getError(), "TboGetBookingDetail", detailUrl);

        return result;
    }

    // ─── Fallback Methods ─────────────────────────────────────────────────────

    public List<HotelDto> searchHotelsFallback(HotelSearchRequest req, Throwable t) {
        log.error("[TBO] Circuit breaker — searchHotels: {}", t.getMessage());
        throw new AggregatorException("TBO hotel search is currently unavailable. Please try again later.");
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

    private HotelDto mapToHotelDto(TboHotelSearchResponse.TboHotelResult h, String traceId) {
        BigDecimal price = h.getPrice() != null ? h.getPrice().getOfferedPrice() : null;
        String currency = h.getPrice() != null ? h.getPrice().getCurrencyCode() : "INR";

        return HotelDto.builder()
                .name(h.getHotelName())
                .city("")
                .country("")
                .address(h.getHotelAddress())
                .starRating(h.getStarRating())
                .imageUrls(h.getHotelPicture() != null ? List.of(h.getHotelPicture()) : List.of())
                .amenities(List.of())
                .startingPrice(price)
                .currency(currency)
                .aggregatorSource("TBO")
                .externalHotelId(h.getHotelCode())
                .resultIndex(h.getResultIndex())
                .traceId(traceId)
                .build();
    }

    private HotelDetailDto mapToHotelDetailDto(TboHotelInfoResponse.HotelDetails d) {
        List<String> images = d.getImages() != null ? d.getImages() : List.of();
        List<String> facilities = d.getHotelFacilities() != null ? d.getHotelFacilities() : List.of();

        return HotelDetailDto.builder()
                .name(d.getHotelName())
                .country(d.getCountryName())
                .address(d.getAddress())
                .description(d.getDescription())
                .starRating(d.getStarRating())
                .amenities(facilities)
                .imageUrls(images)
                .build();
    }

    private RoomAvailabilityDto mapToRoomAvailabilityDto(
            TboGetRoomResponse.TboHotelRoomDetail r, long nights) {

        BigDecimal pricePerNight = r.getPrice() != null ? r.getPrice().getOfferedPrice() : BigDecimal.ZERO;
        String currency = r.getPrice() != null ? r.getPrice().getCurrencyCode() : "INR";

        return RoomAvailabilityDto.builder()
                .roomType(r.getRoomTypeName())
                .pricePerNight(pricePerNight)
                .totalPrice(pricePerNight.multiply(BigDecimal.valueOf(nights)))
                .currency(currency)
                .available("Confirm".equalsIgnoreCase(r.getAvailabilityType()))
                .description(r.getRoomDescription())
                .amenities(r.getAmenity() != null ? r.getAmenity() : List.of())
                .roomIndex(r.getRoomIndex())
                .roomTypeCode(r.getRoomTypeCode())
                .roomTypeName(r.getRoomTypeName())
                .ratePlanCode(r.getRatePlanCode())
                .availabilityType(r.getAvailabilityType())
                .cancellationPolicy(r.getCancellationPolicy())
                .lastCancellationDate(r.getLastCancellationDate())
                .isPANMandatory(r.isPANMandatory())
                .isPassportMandatory(r.isPassportMandatory())
                .roomPromotion(r.getRoomPromotion())
                .build();
    }

    // ─── HTTP helpers ─────────────────────────────────────────────────────────

    /**
     * Posts to a TBO endpoint and parses the JSON response.
     *
     * Always reads the raw response body as a String first so we can log it and detect
     * HTML error pages before Jackson attempts deserialization. This prevents the cryptic
     * "no HttpMessageConverter for text/html" error and gives full visibility into what
     * TBO actually returned.
     */
    private <T> T post(String url, Object requestBody, Class<T> responseType) {
        HttpHeaders headers = new HttpHeaders();
        headers.setContentType(MediaType.APPLICATION_JSON);
        // Accept anything so RestTemplate never rejects TBO's content-type header
        headers.setAccept(List.of(MediaType.ALL));
        HttpEntity<Object> entity = new HttpEntity<>(requestBody, headers);

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
