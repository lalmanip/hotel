package com.vivance.hotel.security;

import lombok.Data;

@Data
public class JwtAuthToken {
    private String token;
    private String userSessionId;
    private String apiKey;
    private String ipAddress;
}
