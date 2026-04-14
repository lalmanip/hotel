package com.vivance.hotel.infrastructure.aggregator.tbo.dto;

import com.fasterxml.jackson.annotation.JsonProperty;
import lombok.Builder;
import lombok.Data;

/** Passenger detail sent in the TBO Book request. */
@Data
@Builder
public class TboHotelPassenger {

    /** Mr, Mrs, Miss, Ms */
    @JsonProperty("Title")
    private String title;

    @JsonProperty("FirstName")
    private String firstName;

    @JsonProperty("Middlename")
    private String middleName;

    @JsonProperty("LastName")
    private String lastName;

    @JsonProperty("Phoneno")
    private String phoneNo;

    @JsonProperty("Email")
    private String email;

    /**
     * PaxType: 1 = Adult, 2 = Child
     */
    @JsonProperty("PaxType")
    @Builder.Default
    private int paxType = 1;

    @JsonProperty("LeadPassenger")
    @Builder.Default
    private boolean leadPassenger = true;

    @JsonProperty("Age")
    @Builder.Default
    private int age = 0;

    @JsonProperty("PassportNo")
    private String passportNo;

    @JsonProperty("PassportIssueDate")
    @Builder.Default
    private String passportIssueDate = "0001-01-01T00:00:00";

    @JsonProperty("PassportExpDate")
    @Builder.Default
    private String passportExpDate = "0001-01-01T00:00:00";

    @JsonProperty("PAN")
    private String pan;
}
