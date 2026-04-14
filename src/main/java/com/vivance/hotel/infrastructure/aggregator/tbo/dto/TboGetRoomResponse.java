package com.vivance.hotel.infrastructure.aggregator.tbo.dto;

import com.fasterxml.jackson.annotation.JsonProperty;
import lombok.Data;

import java.time.LocalDateTime;
import java.util.List;

/** Response from POST /hotelservice.svc/rest/GetHotelRoom */
@Data
public class TboGetRoomResponse {

    @JsonProperty("GetHotelRoomResult")
    private GetHotelRoomResult getHotelRoomResult;

    @Data
    public static class GetHotelRoomResult {

        @JsonProperty("ResponseStatus")
        private int responseStatus;

        @JsonProperty("Error")
        private TboError error;

        @JsonProperty("TraceId")
        private String traceId;

        @JsonProperty("IsUnderCancellationAllowed")
        private boolean isUnderCancellationAllowed;

        @JsonProperty("IsPolicyPerStay")
        private boolean isPolicyPerStay;

        @JsonProperty("HotelRoomsDetails")
        private List<TboHotelRoomDetail> hotelRoomsDetails;

        @JsonProperty("RoomCombinations")
        private RoomCombinations roomCombinations;
    }

    @Data
    public static class TboHotelRoomDetail {

        /** "Confirm" = instantly confirmed, "OnRequest" = needs manual confirmation */
        @JsonProperty("AvailabilityType")
        private String availabilityType;

        @JsonProperty("ChildCount")
        private int childCount;

        @JsonProperty("IsTransferIncluded")
        private boolean isTransferIncluded;

        @JsonProperty("RequireAllPaxDetails")
        private boolean requireAllPaxDetails;

        @JsonProperty("RoomId")
        private int roomId;

        @JsonProperty("RoomStatus")
        private int roomStatus;

        @JsonProperty("RoomIndex")
        private int roomIndex;

        @JsonProperty("RoomTypeCode")
        private String roomTypeCode;

        @JsonProperty("RoomDescription")
        private String roomDescription;

        @JsonProperty("RoomTypeName")
        private String roomTypeName;

        @JsonProperty("RatePlanCode")
        private String ratePlanCode;

        @JsonProperty("RatePlan")
        private int ratePlan;

        @JsonProperty("InfoSource")
        private String infoSource;

        @JsonProperty("SequenceNo")
        private String sequenceNo;

        @JsonProperty("DayRates")
        private List<TboDayRate> dayRates;

        @JsonProperty("IsPerStay")
        private boolean isPerStay;

        @JsonProperty("Price")
        private TboPrice price;

        @JsonProperty("RoomPromotion")
        private String roomPromotion;

        @JsonProperty("Amenities")
        private List<String> amenities;

        @JsonProperty("Amenity")
        private List<String> amenity;

        @JsonProperty("SmokingPreference")
        private String smokingPreference;

        @JsonProperty("BedTypes")
        private List<Object> bedTypes;

        @JsonProperty("HotelSupplements")
        private List<TboHotelSupplement> hotelSupplements;

        @JsonProperty("LastCancellationDate")
        private LocalDateTime lastCancellationDate;

        @JsonProperty("CancellationPolicies")
        private List<TboCancellationPolicy> cancellationPolicies;

        @JsonProperty("LastVoucherDate")
        private LocalDateTime lastVoucherDate;

        @JsonProperty("CancellationPolicy")
        private String cancellationPolicy;

        @JsonProperty("Inclusion")
        private List<String> inclusion;

        @JsonProperty("IsPassportMandatory")
        private boolean isPassportMandatory;

        @JsonProperty("IsPANMandatory")
        private boolean isPANMandatory;
    }

    @Data
    public static class RoomCombinations {
        @JsonProperty("InfoSource")
        private String infoSource;
        @JsonProperty("IsPolicyPerStay")
        private boolean isPolicyPerStay;
        @JsonProperty("RoomCombination")
        private List<RoomCombination> roomCombination;
    }

    @Data
    public static class RoomCombination {
        @JsonProperty("RoomIndex")
        private List<Integer> roomIndex;
    }
}
