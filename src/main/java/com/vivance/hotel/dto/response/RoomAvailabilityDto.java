package com.vivance.hotel.dto.response;

import lombok.Builder;
import lombok.Data;

import java.math.BigDecimal;
import java.time.LocalDateTime;
import java.util.List;

/**
 * Room availability DTO returned from GetHotelRoom.
 *
 * <p><b>TBO booking fields:</b> {@code roomIndex}, {@code roomTypeCode}, {@code roomTypeName},
 * {@code ratePlanCode}, and {@code price} must be echoed verbatim into the Block and Book
 * request bodies. Clients must preserve these values from this response.
 */
@Data
@Builder
public class RoomAvailabilityDto {

    private Long id;
    private String roomType;
    private Integer maxOccupancy;
    private BigDecimal pricePerNight;
    private BigDecimal totalPrice;
    private String currency;
    private boolean available;
    private String description;
    private List<String> amenities;
    private List<String> imageUrls;

    // ── TBO session fields — must be sent back in Block / Book ────────────

    /** TBO RoomIndex — identifies this specific room option within the search result. */
    private Integer roomIndex;

    /** TBO RoomTypeCode — e.g. "215713793|258053627$37321". */
    private String roomTypeCode;

    /** TBO RoomTypeName — human-readable room type name. */
    private String roomTypeName;

    /** TBO RatePlanCode — e.g. "215713793|258053627|37321|23". */
    private String ratePlanCode;

    /** "Confirm" = instant, "OnRequest" = needs manual confirmation. */
    private String availabilityType;

    /** Cancellation policy in human-readable text. */
    private String cancellationPolicy;

    /** Last date to cancel without penalty. */
    private LocalDateTime lastCancellationDate;

    /** Whether a PAN card is required to complete this booking. */
    private boolean isPANMandatory;

    /** Whether a passport is required to complete this booking. */
    private boolean isPassportMandatory;

    /** Room promotions e.g. "Save:10%". */
    private String roomPromotion;
}
