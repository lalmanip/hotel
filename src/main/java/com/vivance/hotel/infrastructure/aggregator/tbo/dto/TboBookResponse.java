package com.vivance.hotel.infrastructure.aggregator.tbo.dto;

import com.fasterxml.jackson.annotation.JsonProperty;
import lombok.Data;

/** Response from POST /hotelservice.svc/rest/book */
@Data
public class TboBookResponse {

    @JsonProperty("BookResult")
    private BookResult bookResult;

    @Data
    public static class BookResult {

        @JsonProperty("TBOReferenceNo")
        private String tboReferenceNo;

        @JsonProperty("VoucherStatus")
        private boolean voucherStatus;

        @JsonProperty("ResponseStatus")
        private int responseStatus;

        @JsonProperty("Error")
        private TboError error;

        @JsonProperty("TraceId")
        private String traceId;

        /**
         * Booking status:
         * 1 = Confirmed, 2 = Failed, 3 = VerifyPrice, 6 = Cancelled
         */
        @JsonProperty("Status")
        private int status;

        /** "Confirmed", "BookFailed", "VerifyPrice", "Cancelled" */
        @JsonProperty("HotelBookingStatus")
        private String hotelBookingStatus;

        @JsonProperty("InvoiceNumber")
        private String invoiceNumber;

        @JsonProperty("ConfirmationNo")
        private String confirmationNo;

        @JsonProperty("BookingRefNo")
        private String bookingRefNo;

        @JsonProperty("BookingId")
        private long bookingId;

        @JsonProperty("IsPriceChanged")
        private boolean isPriceChanged;

        @JsonProperty("IsCancellationPolicyChanged")
        private boolean isCancellationPolicyChanged;
    }
}
