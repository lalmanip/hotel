package com.vivance.hotel.infrastructure.aggregator.tbo.dto;

import com.fasterxml.jackson.annotation.JsonProperty;
import lombok.Data;

/**
 * Response from the TBO authentication endpoint.
 */
@Data
public class TboAuthResponse {

    @JsonProperty("TokenId")
    private String tokenId;

    @JsonProperty("IsSuccess")
    private boolean success;

    @JsonProperty("Error")
    private TboError error;

    @Data
    public static class TboError {

        @JsonProperty("ErrorCode")
        private String errorCode;

        @JsonProperty("ErrorMessage")
        private String errorMessage;
    }
}
