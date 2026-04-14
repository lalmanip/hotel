package com.vivance.hotel.exception;

import org.springframework.http.HttpStatus;
import org.springframework.web.bind.annotation.ResponseStatus;

@ResponseStatus(HttpStatus.SERVICE_UNAVAILABLE)
public class AggregatorException extends RuntimeException {

    public AggregatorException(String message) {
        super(message);
    }

    public AggregatorException(String message, Throwable cause) {
        super(message, cause);
    }
}
