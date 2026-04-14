package com.vivance.hotel.dto.response;

import lombok.Builder;
import lombok.Data;

import java.math.BigDecimal;
import java.util.List;

/**
 * Lightweight hotel DTO returned in search results.
 * Normalized across all aggregators into one common format.
 *
 * <p><b>TBO session fields:</b> {@code traceId} and {@code resultIndex} are returned by
 * TBO's search API and must be echoed back in all subsequent calls
 * (hotel info, room availability, block, book) for the same search session.
 * Clients must store these values and include them in follow-up requests.
 */
@Data
@Builder
public class HotelDto {

    private Long id;
    private String name;
    private String city;
    private String country;
    private String address;
    private Integer starRating;
    private List<String> amenities;
    private List<String> imageUrls;

    /** Lowest available room price (OfferedPrice from TBO) for the requested dates. */
    private BigDecimal startingPrice;
    private String currency;

    /** Which aggregator sourced this result — e.g. "TBO". */
    private String aggregatorSource;

    /** The hotel's ID within the aggregator system (HotelCode in TBO). */
    private String externalHotelId;

    /**
     * TBO-specific: ResultIndex from search response.
     * Required for GetHotelInfo, GetHotelRoom, Block, and Book calls.
     */
    private Integer resultIndex;

    /**
     * TBO-specific: TraceId from the search response.
     * Ties all API calls in one search session together.
     * Required for GetHotelInfo, GetHotelRoom, Block, and Book calls.
     */
    private String traceId;
}
