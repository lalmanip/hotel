package com.vivance.hotel.dto.request;

import jakarta.validation.Valid;
import jakarta.validation.constraints.Max;
import jakarta.validation.constraints.Min;
import jakarta.validation.constraints.NotEmpty;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Size;
import lombok.Data;

import java.time.LocalDate;
import java.util.List;

@Data
public class HotelSearchV1Request {

    /**
     * Destination city identifier. In this service it maps to {@code tbo_hotels_static.city_code}.
     * Optional when {@link #hotelCode} is provided.
     */
    private Long destinationId;

    /** Optional: direct hotel search. */
    @Size(max = 50)
    private String hotelCode;

    @NotNull
    private LocalDate checkIn;

    @NotNull
    private LocalDate checkOut;

    @NotEmpty
    @Valid
    private List<Room> rooms;

    @Valid
    private Filters filters;

    @NotNull
    @Valid
    private Pagination pagination;

    public boolean hasHotelCode() {
        return hotelCode != null && !hotelCode.isBlank();
    }

    public boolean hasDestination() {
        return destinationId != null && destinationId > 0;
    }

    @Data
    public static class Room {
        @NotNull
        @Min(1)
        @Max(10)
        private Integer adults;

        /**
         * Child ages (e.g. [3, 7]). Empty list means no children.
         */
        private List<@Min(0) @Max(17) Integer> children;
    }

    @Data
    public static class Filters {
        @Min(0)
        private Double priceMin;

        @Min(0)
        private Double priceMax;

        /** Star ratings list, e.g. [3,4,5]. */
        private List<@Min(1) @Max(5) Integer> stars;
    }

    @Data
    public static class Pagination {
        @NotNull
        @Min(1)
        private Integer page;

        @NotNull
        @Min(1)
        @Max(500)
        private Integer size;
    }
}

