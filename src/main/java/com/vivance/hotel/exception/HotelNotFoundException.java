package com.vivance.hotel.exception;

public class HotelNotFoundException extends ResourceNotFoundException {

    public HotelNotFoundException(Long id) {
        super("Hotel", id);
    }

    public HotelNotFoundException(String message) {
        super(message);
    }
}
