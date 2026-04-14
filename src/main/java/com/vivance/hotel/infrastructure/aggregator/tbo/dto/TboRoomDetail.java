package com.vivance.hotel.infrastructure.aggregator.tbo.dto;

import com.fasterxml.jackson.annotation.JsonProperty;
import lombok.Builder;
import lombok.Data;

import java.util.List;

/**
 * Room detail element used in both Block and Book request bodies.
 * Values must be taken verbatim from the GetHotelRoom response.
 */
@Data
@Builder
public class TboRoomDetail {

    @JsonProperty("RoomIndex")
    private String roomIndex;

    @JsonProperty("RoomTypeCode")
    private String roomTypeCode;

    @JsonProperty("RoomTypeName")
    private String roomTypeName;

    @JsonProperty("RatePlanCode")
    private String ratePlanCode;

    @JsonProperty("BedTypeCode")
    private String bedTypeCode;

    /** 0 = NoPreference, 1 = Smoking, 2 = NonSmoking, 3 = Either */
    @JsonProperty("SmokingPreference")
    @Builder.Default
    private int smokingPreference = 0;

    @JsonProperty("Supplements")
    private List<Object> supplements;

    @JsonProperty("Price")
    private TboPrice price;

    /** Populated for Book step only; null for Block. */
    @JsonProperty("HotelPassenger")
    private List<TboHotelPassenger> hotelPassenger;
}
