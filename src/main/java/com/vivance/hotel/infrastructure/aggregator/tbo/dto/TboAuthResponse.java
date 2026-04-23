package com.vivance.hotel.infrastructure.aggregator.tbo.dto;

import com.fasterxml.jackson.annotation.JsonIgnoreProperties;
import com.fasterxml.jackson.annotation.JsonProperty;
import lombok.Data;

/**
 * Response from the TBO authentication endpoint.
 */
@Data
@JsonIgnoreProperties(ignoreUnknown = true)
public class TboAuthResponse {

    /** 1 = success (per TBO). */
    @JsonProperty("Status")
    private Integer status;

    @JsonProperty("TokenId")
    private String tokenId;

    @JsonProperty("Error")
    private TboError error;

    @JsonProperty("Member")
    private Member member;

    public boolean isSuccess() {
        return status != null && status == 1 && tokenId != null && !tokenId.isBlank();
    }

    @Data
    @JsonIgnoreProperties(ignoreUnknown = true)
    public static class TboError {

        @JsonProperty("ErrorCode")
        private Integer errorCode;

        @JsonProperty("ErrorMessage")
        private String errorMessage;
    }

    @Data
    @JsonIgnoreProperties(ignoreUnknown = true)
    public static class Member {
        @JsonProperty("FirstName")
        private String firstName;
        @JsonProperty("LastName")
        private String lastName;
        @JsonProperty("Email")
        private String email;
        @JsonProperty("MemberId")
        private Long memberId;
        @JsonProperty("AgencyId")
        private Long agencyId;
        @JsonProperty("LoginName")
        private String loginName;
        @JsonProperty("LoginDetails")
        private String loginDetails;
        @JsonProperty("isPrimaryAgent")
        private Boolean isPrimaryAgent;
    }
}
