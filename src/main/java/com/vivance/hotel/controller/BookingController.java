package com.vivance.hotel.controller;

import com.vivance.hotel.dto.request.BookingRequest;
import com.vivance.hotel.dto.response.ApiResponse;
import com.vivance.hotel.dto.response.BookingDto;
import com.vivance.hotel.service.BookingService;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.security.SecurityRequirement;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.security.core.userdetails.UserDetails;
import org.springframework.web.bind.annotation.*;

import java.util.List;

@Slf4j
@RestController
@RequestMapping("/api/v1/bookings")
@RequiredArgsConstructor
@Tag(name = "Bookings", description = "Create and manage hotel bookings")
@SecurityRequirement(name = "bearerAuth")
public class BookingController {

    private final BookingService bookingService;

    /**
     * POST /api/v1/bookings
     * Creates a new booking for the authenticated user.
     */
    @PostMapping
    @Operation(summary = "Create a new hotel booking")
    public ResponseEntity<ApiResponse<BookingDto>> createBooking(
            @Valid @RequestBody BookingRequest request,
            @AuthenticationPrincipal UserDetails userDetails) {

        log.info("Booking request from user={} for hotel={}", userDetails.getUsername(), request.getHotelId());
        BookingDto booking = bookingService.createBooking(request, userDetails.getUsername());
        return ResponseEntity.status(HttpStatus.CREATED)
                .body(ApiResponse.success("Booking created successfully", booking));
    }

    /**
     * GET /api/v1/bookings
     * Returns all bookings for the authenticated user.
     */
    @GetMapping
    @Operation(summary = "Get all bookings for the authenticated user")
    public ResponseEntity<ApiResponse<List<BookingDto>>> getUserBookings(
            @AuthenticationPrincipal UserDetails userDetails) {

        List<BookingDto> bookings = bookingService.getUserBookings(userDetails.getUsername());
        return ResponseEntity.ok(ApiResponse.success(bookings));
    }

    /**
     * GET /api/v1/bookings/{id}
     * Returns a specific booking by ID (only accessible by the owner).
     */
    @GetMapping("/{id}")
    @Operation(summary = "Get booking details by ID")
    public ResponseEntity<ApiResponse<BookingDto>> getBooking(
            @PathVariable Long id,
            @AuthenticationPrincipal UserDetails userDetails) {

        BookingDto booking = bookingService.getBooking(id, userDetails.getUsername());
        return ResponseEntity.ok(ApiResponse.success(booking));
    }

    /**
     * DELETE /api/v1/bookings/{id}
     * Cancels a booking (must be future, must be owner).
     */
    @DeleteMapping("/{id}")
    @Operation(summary = "Cancel a booking")
    public ResponseEntity<ApiResponse<BookingDto>> cancelBooking(
            @PathVariable Long id,
            @AuthenticationPrincipal UserDetails userDetails) {

        BookingDto cancelled = bookingService.cancelBooking(id, userDetails.getUsername());
        return ResponseEntity.ok(ApiResponse.success("Booking cancelled successfully", cancelled));
    }
}
