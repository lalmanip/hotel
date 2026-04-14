package com.vivance.hotel.infrastructure.aggregator.tbo.dto;

import com.fasterxml.jackson.annotation.JsonProperty;
import lombok.AllArgsConstructor;
import lombok.Data;

/**
 * Request payload for TBO authentication endpoint.
 * Field names match the TBO API contract exactly.
 */
@Data
@AllArgsConstructor
public class TboAuthRequest {

    @JsonProperty("ClientId")
    private String clientId;

    @JsonProperty("UserName")
    private String userName;

    @JsonProperty("Password")
    private String password;

    @JsonProperty("EndUserIp")
    private String endUserIp;
}
