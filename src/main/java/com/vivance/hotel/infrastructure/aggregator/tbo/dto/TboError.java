package com.vivance.hotel.infrastructure.aggregator.tbo.dto;

import com.fasterxml.jackson.annotation.JsonProperty;
import lombok.Data;

/** Common error wrapper returned by every TBO endpoint. ErrorCode 0 = no error. */
@Data
public class TboError {

    @JsonProperty("ErrorCode")
    private int errorCode;

    @JsonProperty("ErrorMessage")
    private String errorMessage;

    public boolean hasError() {
        return errorCode != 0;
    }
}
