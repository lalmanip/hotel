package com.vivance.hotel.infrastructure.aggregator.tbo.dto;

import com.fasterxml.jackson.annotation.JsonIgnoreProperties;
import com.fasterxml.jackson.annotation.JsonProperty;
import lombok.Data;

import java.math.BigDecimal;
import java.util.List;

@Data
@JsonIgnoreProperties(ignoreUnknown = true)
public class TboAffiliateSearchResponse {

    @JsonProperty("Status")
    private TboAffiliateStatus status;

    @JsonProperty("HotelResult")
    private List<HotelResult> hotelResult;

    @Data
    @JsonIgnoreProperties(ignoreUnknown = true)
    public static class HotelResult {
        @JsonProperty("HotelCode")
        private String hotelCode;

        @JsonProperty("Currency")
        private String currency;

        @JsonProperty("Rooms")
        private List<Room> rooms;

        @JsonProperty("RateConditions")
        private List<String> rateConditions;
    }

    @Data
    @JsonIgnoreProperties(ignoreUnknown = true)
    public static class Room {
        @JsonProperty("Name")
        private List<String> name;

        @JsonProperty("BookingCode")
        private String bookingCode;

        @JsonProperty("Inclusion")
        private String inclusion;

        @JsonProperty("DayRates")
        private List<List<BasePriceDay>> dayRates;

        @JsonProperty("TotalFare")
        private BigDecimal totalFare;

        @JsonProperty("TotalTax")
        private BigDecimal totalTax;

        @JsonProperty("RoomID")
        private List<String> roomId;

        @JsonProperty("RoomPromotion")
        private List<String> roomPromotion;

        @JsonProperty("CancelPolicies")
        private List<CancelPolicy> cancelPolicies;

        @JsonProperty("MealType")
        private String mealType;

        @JsonProperty("IsRefundable")
        private Boolean isRefundable;

        @JsonProperty("Supplements")
        private List<List<Supplement>> supplements;

        @JsonProperty("WithTransfers")
        private Boolean withTransfers;

        @JsonProperty("Amenities")
        private List<String> amenities;
    }

    @Data
    @JsonIgnoreProperties(ignoreUnknown = true)
    public static class BasePriceDay {
        @JsonProperty("BasePrice")
        private BigDecimal basePrice;
    }

    @Data
    @JsonIgnoreProperties(ignoreUnknown = true)
    public static class CancelPolicy {
        @JsonProperty("Index")
        private String index;

        @JsonProperty("FromDate")
        private String fromDate;

        @JsonProperty("ChargeType")
        private String chargeType;

        @JsonProperty("CancellationCharge")
        private BigDecimal cancellationCharge;
    }

    @Data
    @JsonIgnoreProperties(ignoreUnknown = true)
    public static class Supplement {
        @JsonProperty("Index")
        private Integer index;

        @JsonProperty("Type")
        private String type;

        @JsonProperty("Description")
        private String description;

        @JsonProperty("Price")
        private BigDecimal price;

        @JsonProperty("Currency")
        private String currency;
    }
}
