package com.vivance.hotel.infrastructure.aggregator.tbo.dto;

import com.fasterxml.jackson.annotation.JsonInclude;
import com.fasterxml.jackson.annotation.JsonProperty;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.math.BigDecimal;
import java.util.List;

@Data
@Builder
@JsonInclude(JsonInclude.Include.NON_NULL)
public class TboAffiliateBookRequest {

    @JsonProperty("BookingCode")
    private String bookingCode;

    @JsonProperty("IsVoucherBooking")
    private Boolean isVoucherBooking;

    @JsonProperty("GuestNationality")
    private String guestNationality;

    @JsonProperty("EndUserIp")
    private String endUserIp;

    @JsonProperty("RequestedBookingMode")
    private Integer requestedBookingMode;

    @JsonProperty("NetAmount")
    private BigDecimal netAmount;

    @JsonProperty("ClientReferenceId")
    private String clientReferenceId;

    @JsonProperty("HotelRoomsDetails")
    private List<HotelRoomDetail> hotelRoomsDetails;

    @Data
    @Builder
    @NoArgsConstructor
    @AllArgsConstructor
    @JsonInclude(JsonInclude.Include.NON_NULL)
    public static class HotelRoomDetail {
        @JsonProperty("HotelPassenger")
        private List<Passenger> hotelPassenger;
    }

    @Data
    @Builder
    @NoArgsConstructor
    @AllArgsConstructor
    @JsonInclude(JsonInclude.Include.NON_NULL)
    public static class Passenger {
        @JsonProperty("Title")
        private String title;
        @JsonProperty("FirstName")
        private String firstName;
        @JsonProperty("MiddleName")
        private String middleName;
        @JsonProperty("LastName")
        private String lastName;
        @JsonProperty("Email")
        private String email;
        @JsonProperty("PaxType")
        private Integer paxType;
        @JsonProperty("LeadPassenger")
        private Boolean leadPassenger;
        @JsonProperty("Age")
        private Integer age;
        @JsonProperty("PassportNo")
        private String passportNo;
        @JsonProperty("PassportIssueDate")
        private String passportIssueDate;
        @JsonProperty("PassportExpDate")
        private String passportExpDate;
        @JsonProperty("Phoneno")
        private String phoneno;
        @JsonProperty("PaxId")
        private Integer paxId;
        @JsonProperty("GSTCompanyAddress")
        private String gstCompanyAddress;
        @JsonProperty("GSTCompanyContactNumber")
        private String gstCompanyContactNumber;
        @JsonProperty("GSTCompanyName")
        private String gstCompanyName;
        @JsonProperty("GSTNumber")
        private String gstNumber;
        @JsonProperty("GSTCompanyEmail")
        private String gstCompanyEmail;
        @JsonProperty("PAN")
        private String pan;
    }
}
