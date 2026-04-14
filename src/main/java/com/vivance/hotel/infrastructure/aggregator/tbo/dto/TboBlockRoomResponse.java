package com.vivance.hotel.infrastructure.aggregator.tbo.dto;

import com.fasterxml.jackson.annotation.JsonProperty;
import lombok.Data;

import java.time.LocalDateTime;
import java.util.List;

/** Response from POST /hotelservice.svc/rest/blockRoom */
@Data
public class TboBlockRoomResponse {

    @JsonProperty("BlockRoomResult")
    private BlockRoomResult blockRoomResult;

    @Data
    public static class BlockRoomResult {

        @JsonProperty("ResponseStatus")
        private int responseStatus;

        @JsonProperty("Error")
        private TboError error;

        @JsonProperty("TraceId")
        private String traceId;

        @JsonProperty("IsPriceChanged")
        private boolean isPriceChanged;

        @JsonProperty("IsCancellationPolicyChanged")
        private boolean isCancellationPolicyChanged;

        @JsonProperty("IsUnderCancellationAllowed")
        private boolean isUnderCancellationAllowed;

        @JsonProperty("IsVoucherBooking")
        private boolean isVoucherBooking;

        @JsonProperty("HotelRoomsDetails")
        private List<BlockedRoomDetail> hotelRoomsDetails;

        @JsonProperty("ValidationInfo")
        private ValidationInfo validationInfo;
    }

    @Data
    public static class BlockedRoomDetail {

        @JsonProperty("RoomIndex")
        private int roomIndex;

        @JsonProperty("RoomTypeCode")
        private String roomTypeCode;

        @JsonProperty("RoomTypeName")
        private String roomTypeName;

        @JsonProperty("RatePlanCode")
        private String ratePlanCode;

        @JsonProperty("Price")
        private TboPrice price;

        @JsonProperty("LastCancellationDate")
        private LocalDateTime lastCancellationDate;

        @JsonProperty("CancellationPolicies")
        private List<TboCancellationPolicy> cancellationPolicies;

        @JsonProperty("CancellationPolicy")
        private String cancellationPolicy;

        @JsonProperty("IsPassportMandatory")
        private boolean isPassportMandatory;

        @JsonProperty("IsPANMandatory")
        private boolean isPANMandatory;

        @JsonProperty("HotelSupplements")
        private List<TboHotelSupplement> hotelSupplements;
    }

    @Data
    public static class ValidationInfo {
        @JsonProperty("ValidationAtConfirm")
        private ValidationDetail validationAtConfirm;
        @JsonProperty("ValidationAtVoucher")
        private ValidationDetail validationAtVoucher;
    }

    @Data
    public static class ValidationDetail {
        @JsonProperty("IsPANMandatory")        private boolean isPANMandatory;
        @JsonProperty("IsPassportMandatory")   private boolean isPassportMandatory;
        @JsonProperty("IsSamePANForAllAllowed")private boolean isSamePANForAllAllowed;
        @JsonProperty("IsEmailMandatory")      private boolean isEmailMandatory;
        @JsonProperty("NoOfPANRequired")       private int noOfPANRequired;
    }
}
