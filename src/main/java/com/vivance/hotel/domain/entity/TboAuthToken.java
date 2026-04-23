package com.vivance.hotel.domain.entity;

import jakarta.persistence.*;
import lombok.*;

import java.time.LocalDate;
import java.time.LocalDateTime;

/**
 * Persists TBO authentication tokens.
 * TBO issues one token per calendar day (Asia/Kolkata timezone).
 * We cache it here so we authenticate at most once per day regardless
 * of how many requests are made.
 */
@Entity
@Table(
    name = "tbo_auth_token",
    indexes = {
        @Index(name = "idx_tbo_token_date", columnList = "token_date", unique = true)
    }
)
@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class TboAuthToken {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    /** The actual TBO token string used in API request headers. */
    @Column(name = "token_id", nullable = false, length = 500)
    private String tokenId;

    /** The IST calendar date this token is valid for. */
    @Column(name = "token_date", nullable = false, unique = true)
    private LocalDate tokenDate;

    /** When we obtained and stored this token. */
    @Column(name = "created_at", nullable = false)
    private LocalDateTime createdAt;
}
