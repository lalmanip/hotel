package com.vivance.hotel.dto.response;

import lombok.Builder;
import lombok.Data;

import java.util.List;

@Data
@Builder
public class TboHotelStaticDetailsSyncResult {

    /** All rows in {@code tbo_hotels_static}. */
    private long totalRowsInTboHotelsStatic;
    /**
     * Rows that still need a Hoteldetails sync at the start of this run (no {@code tbo_hotel_static_details}
     * row with non-null {@code fetched_at}), after applying the city filter when configured.
     */
    private long pendingHotelCodesAtStart;
    /** When non-empty, only these city names (case-insensitive) were eligible; empty means all cities. */
    private List<String> cityFilterNormalized;
    private int apiBatchesAttempted;
    private int hotelsSavedOrUpdated;
    private int batchesFailed;
    private long durationMs;
    private List<String> errorSamples;
}
