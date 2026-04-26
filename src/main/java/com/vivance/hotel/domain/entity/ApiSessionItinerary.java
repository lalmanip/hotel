package com.vivance.hotel.domain.entity;

import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.GeneratedValue;
import jakarta.persistence.GenerationType;
import jakarta.persistence.Id;
import jakarta.persistence.Lob;
import jakarta.persistence.Table;
import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.time.Instant;

@Entity
@Table(name = "api_session_itinerary")
@Data
@NoArgsConstructor
@AllArgsConstructor
public class ApiSessionItinerary {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    @Column(name = "id")
    private Long id;

    @Lob
    @Column(name = "amadeus_session")
    private String amadeusSession;

    @Column(name = "selected_result_token")
    private String selectedResultToken;

    @Column(name = "app_booking_ref_id")
    private String appBookingRefId;

    @Column(name = "pnr_number")
    private String pnrNumber;

    @Column(name = "pnr_provider")
    private String pnrProvider;

    @Lob
    @Column(name = "selected_journey")
    private String selectedJourney;

    @Column(name = "progress_step")
    private String progressStep;

    @Lob
    @Column(name = "progress_itinerary")
    private String progressItinerary;

    /**
     * Trace id for correlating with request logs (same value returned in X-Trace-Id).
     */
    @Column(name = "trace_id")
    private String traceId;

    /**
     * DB-managed defaults: created_time defaults to CURRENT_TIMESTAMP.
     * update_time is nullable and can be set by the app when updating.
     */
    @Column(name = "created_time", insertable = false, updatable = false)
    private Instant createdTime;

    @Column(name = "update_time")
    private Instant updateTime;
}

