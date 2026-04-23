package com.vivance.hotel.infrastructure.aggregator.tbo.dto;

import com.fasterxml.jackson.annotation.JsonIgnoreProperties;
import com.fasterxml.jackson.annotation.JsonProperty;
import lombok.Data;

import java.util.List;

@Data
@JsonIgnoreProperties(ignoreUnknown = true)
public class TboAffiliatePreBookResponse {

    @JsonProperty("Status")
    private TboAffiliateStatus status;

    @JsonProperty("HotelResult")
    private List<TboAffiliateSearchResponse.HotelResult> hotelResult;

    @JsonProperty("ValidationInfo")
    private ValidationInfo validationInfo;

    @Data
    @JsonIgnoreProperties(ignoreUnknown = true)
    public static class ValidationInfo {
        @JsonProperty("PanMandatory")
        private Boolean panMandatory;

        @JsonProperty("PassportMandatory")
        private Boolean passportMandatory;

        @JsonProperty("CorporateBookingAllowed")
        private Boolean corporateBookingAllowed;

        @JsonProperty("PanCountRequired")
        private Integer panCountRequired;

        @JsonProperty("SamePaxNameAllowed")
        private Boolean samePaxNameAllowed;

        @JsonProperty("SpaceAllowed")
        private Boolean spaceAllowed;

        @JsonProperty("SpecialCharAllowed")
        private Boolean specialCharAllowed;

        @JsonProperty("PaxNameMinLength")
        private Integer paxNameMinLength;

        @JsonProperty("PaxNameMaxLength")
        private Integer paxNameMaxLength;

        @JsonProperty("CharLimit")
        private Boolean charLimit;

        @JsonProperty("PackageFare")
        private Boolean packageFare;

        @JsonProperty("PackageDetailsMandatory")
        private Boolean packageDetailsMandatory;

        @JsonProperty("DepartureDetailsMandatory")
        private Boolean departureDetailsMandatory;

        @JsonProperty("GSTAllowed")
        private Boolean gstAllowed;
    }
}
