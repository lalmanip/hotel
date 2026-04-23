package com.vivance.hotel.dto.request;

import com.fasterxml.jackson.annotation.JsonInclude;
import com.fasterxml.jackson.annotation.JsonProperty;
import com.vivance.hotel.infrastructure.aggregator.tbo.dto.TboAffiliateBookRequest;
import jakarta.validation.Valid;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import lombok.Data;

import java.math.BigDecimal;
import java.util.List;

@Data
@JsonInclude(JsonInclude.Include.NON_NULL)
public class HotelBookRequest {

    @NotBlank(message = "BookingCode is required")
    @JsonProperty("BookingCode")
    private String bookingCode;

    @NotNull(message = "IsVoucherBooking is required")
    @JsonProperty("IsVoucherBooking")
    private Boolean isVoucherBooking;

    @JsonProperty("GuestNationality")
    private String guestNationality;

    @JsonProperty("EndUserIp")
    private String endUserIp;

    @JsonProperty("TokenId")
    private String tokenId;

    @JsonProperty("RequestedBookingMode")
    private Integer requestedBookingMode;

    @NotNull(message = "NetAmount is required")
    @JsonProperty("NetAmount")
    private BigDecimal netAmount;

    @NotBlank(message = "ClientReferenceId is required")
    @JsonProperty("ClientReferenceId")
    private String clientReferenceId;

    @Valid
    @NotNull(message = "HotelRoomsDetails is required")
    @JsonProperty("HotelRoomsDetails")
    private List<TboAffiliateBookRequest.HotelRoomDetail> hotelRoomsDetails;
}

