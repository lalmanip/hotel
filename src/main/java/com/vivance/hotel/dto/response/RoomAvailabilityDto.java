package com.vivance.hotel.dto.response;

import lombok.Builder;
import lombok.Data;

import java.math.BigDecimal;
import java.time.LocalDateTime;
import java.util.List;

/**
 * Room availability DTO (TBO affiliate Search / PreBook).
 *
 * <p><b>TBO affiliate:</b> {@code roomIndex} holds the supplier {@code BookingCode} for
 * PreBook and Book. Preserve it for the booking flow.
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

    /** TBO room identifier (legacy RoomIndex or affiliate BookingCode). */
    private String roomIndex;

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

    /** TBO affiliate {@code RoomID} values when returned by Search / PreBook. */
    private List<String> tboRoomIds;
}
