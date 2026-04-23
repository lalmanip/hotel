package com.vivance.hotel.infrastructure.aggregator.tbo.dto;

import com.fasterxml.jackson.annotation.JsonAlias;
import com.fasterxml.jackson.annotation.JsonIgnoreProperties;
import com.fasterxml.jackson.annotation.JsonProperty;
import lombok.Data;

import java.util.List;

/** Response from POST {@code .../rest/Getbookingdetail} */
@Data
@JsonIgnoreProperties(ignoreUnknown = true)
public class TboGetBookingDetailResponse {

    @JsonProperty("GetBookingDetailResult")
    private GetBookingDetailResult getBookingDetailResult;

    @Data
    @JsonIgnoreProperties(ignoreUnknown = true)
    public static class GetBookingDetailResult {

        @JsonProperty("VoucherStatus")
        private boolean voucherStatus;

        /**
         * 0=BookFailed, 1=Confirmed, 3=VerifyPrice, 6=Cancelled
         */
        @JsonProperty("Status")
        private int status;

        @JsonProperty("HotelBookingStatus")
        private String hotelBookingStatus;

        @JsonProperty("ResponseStatus")
        private int responseStatus;

        @JsonProperty("Error")
        private TboError error;

        @JsonProperty("TraceId")
        private String traceId;

        @JsonProperty("BookingId")
        private long bookingId;

        @JsonProperty("ConfirmationNo")
        private String confirmationNo;

        /** TBO returns {@code BookingRefNo}; older docs used {@code BookingReferenceNo}. */
        @JsonProperty("BookingRefNo")
        @JsonAlias("BookingReferenceNo")
        private String bookingRefNo;

        @JsonProperty("IsPriceChanged")
        private boolean isPriceChanged;

        @JsonProperty("IsCancellationPolicyChanged")
        private boolean isCancellationPolicyChanged;

        @JsonProperty("HotelName")
        private String hotelName;

        @JsonProperty("StarRating")
        private int starRating;

        @JsonProperty("AddressLine1")
        private String addressLine1;

        @JsonProperty("AddressLine2")
        private String addressLine2;

        @JsonProperty("City")
        private String city;

        @JsonProperty("CheckInDate")
        private String checkInDate;

        @JsonProperty("CheckOutDate")
        private String checkOutDate;

        @JsonProperty("BookingDate")
        private String bookingDate;

        @JsonProperty("NoOfRooms")
        private int noOfRooms;

        @JsonProperty("IsDomestic")
        private boolean isDomestic;

        @JsonProperty("AgentReferenceNo")
        private String agentReferenceNo;

        @JsonProperty("HotelRoomsDetails")
        private List<Object> hotelRoomsDetails;
    }
}
