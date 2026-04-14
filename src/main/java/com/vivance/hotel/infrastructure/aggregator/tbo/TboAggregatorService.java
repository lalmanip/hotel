package com.vivance.hotel.infrastructure.aggregator.tbo;

import com.vivance.hotel.config.AggregatorProperties;
import com.vivance.hotel.domain.enums.AggregatorType;
import com.vivance.hotel.dto.request.HotelSearchRequest;
import com.vivance.hotel.dto.response.BookingDto;
import com.vivance.hotel.dto.response.HotelDetailDto;
import com.vivance.hotel.dto.response.HotelDto;
import com.vivance.hotel.dto.response.RoomAvailabilityDto;
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

        log.info("[TBO] Searching hotels: city={}, checkIn={}, nights={}, guests={}",
                request.getCityId(), request.getCheckIn(), nights, request.getGuests());
        tboApiLogger.logRequest("TboHotelSearch", token, tboReq);

        TboHotelSearchResponse response = post(
                cfg.getBaseUrl() + cfg.getSearchPath(),
                tboReq,
                TboHotelSearchResponse.class
        );
        tboApiLogger.logResponse("TboHotelSearch", token, response);

        TboHotelSearchResponse.HotelSearchResult result = response.getHotelSearchResult();
        validateStatus(result.getResponseStatus(), result.getError(), "TboHotelSearch");

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

        log.info("[TBO] GetHotelInfo: hotelCode={}, resultIndex={}", externalHotelId, resultIndex);
        tboApiLogger.logRequest("TboGetHotelInfo", token, tboReq);

        TboHotelInfoResponse response = post(
                cfg.getBaseUrl() + cfg.getHotelInfoPath(),
                tboReq,
                TboHotelInfoResponse.class
        );
        tboApiLogger.logResponse("TboGetHotelInfo", token, response);

        TboHotelInfoResponse.HotelInfoResult result = response.getHotelInfoResult();
        validateStatus(result.getResponseStatus(), result.getError(), "TboGetHotelInfo");

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

        log.info("[TBO] GetHotelRoom: hotelCode={}, resultIndex={}, traceId={}",
                externalHotelId, resultIndex, traceId);
        tboApiLogger.logRequest("TboGetHotelRoom", token, tboReq);

        TboGetRoomResponse response = post(
                cfg.getBaseUrl() + cfg.getGetRoomPath(),
                tboReq,
                TboGetRoomResponse.class
        );
        tboApiLogger.logResponse("TboGetHotelRoom", token, response);

        TboGetRoomResponse.GetHotelRoomResult result = response.getGetHotelRoomResult();
        validateStatus(result.getResponseStatus(), result.getError(), "TboGetHotelRoom");

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

        log.info("[TBO] BlockRoom: hotelCode={}, resultIndex={}", externalHotelId, resultIndex);
        tboApiLogger.logRequest("TboBlockRoom", token, blockReq);

        TboBlockRoomResponse blockResp = post(
                cfg.getBaseUrl() + cfg.getBlockPath(),
                blockReq,
                TboBlockRoomResponse.class
        );
        tboApiLogger.logResponse("TboBlockRoom", token, blockResp);

        TboBlockRoomResponse.BlockRoomResult blockResult = blockResp.getBlockRoomResult();
        validateStatus(blockResult.getResponseStatus(), blockResult.getError(), "TboBlockRoom");

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

        log.info("[TBO] BookHotel: hotelCode={}, resultIndex={}", externalHotelId, resultIndex);
        tboApiLogger.logRequest("TboBook", token, bookReq);

        TboBookResponse bookResp = post(
                cfg.getBaseUrl() + cfg.getBookPath(),
                bookReq,
                TboBookResponse.class
        );
        tboApiLogger.logResponse("TboBook", token, bookResp);

        TboBookResponse.BookResult bookResult = bookResp.getBookResult();
        validateStatus(bookResult.getResponseStatus(), bookResult.getError(), "TboBook");

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

        log.info("[TBO] GetBookingDetail: bookingId={}", bookingId);
        tboApiLogger.logRequest("TboGetBookingDetail", token, req);

        TboGetBookingDetailResponse response = post(
                cfg.getInternalBaseUrl() + cfg.getBookingDetailPath(),
                req,
                TboGetBookingDetailResponse.class
        );
        tboApiLogger.logResponse("TboGetBookingDetail", token, response);

        TboGetBookingDetailResponse.GetBookingDetailResult result =
                response.getGetBookingDetailResult();
        validateStatus(result.getResponseStatus(), result.getError(), "TboGetBookingDetail");

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

    private <T> T post(String url, Object requestBody, Class<T> responseType) {
        HttpHeaders headers = new HttpHeaders();
        headers.setContentType(MediaType.APPLICATION_JSON);
        HttpEntity<Object> entity = new HttpEntity<>(requestBody, headers);

        ResponseEntity<T> response = tboRestTemplate.exchange(
                url, HttpMethod.POST, entity, responseType);

        if (response.getBody() == null) {
            throw new AggregatorException("Empty response from TBO at: " + url);
        }
        return response.getBody();
    }

    private void validateStatus(int responseStatus, TboError error, String operation) {
        if (responseStatus != 1) {
            String msg = error != null && error.getErrorMessage() != null
                    ? error.getErrorMessage()
                    : "Unknown error (code " + (error != null ? error.getErrorCode() : "?") + ")";
            tboApiLogger.logError(operation, null, msg);
            throw new AggregatorException("[TBO] " + operation + " failed: " + msg);
        }
        if (error != null && error.hasError()) {
            tboApiLogger.logError(operation, null, error.getErrorMessage());
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
