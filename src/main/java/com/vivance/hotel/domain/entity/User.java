package com.vivance.hotel.domain.entity;

import com.vivance.hotel.domain.enums.UserRole;
import jakarta.persistence.*;
import lombok.*;
import org.hibernate.annotations.DynamicInsert;

import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.List;
import java.util.stream.Stream;

/**
 * Partial mapping of the shared Vivance {@code user} table ({@code vivance_java}).
 * Only columns used by the hotel service are mapped; other columns rely on DB defaults
 * (e.g. {@code country_code}, {@code language_preference}, {@code email_activation}).
 */
@Entity
@Table(name = "user")
@DynamicInsert
@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class User {

    /** Values accepted as “active” for login and aggregator calls (trimmed, case-insensitive where noted). */
    private static boolean isActiveStatus(String status) {
        if (status == null || status.isBlank()) {
            return false;
        }
        String s = status.trim();
        return s.equalsIgnoreCase("ACTIVE")
                || s.equalsIgnoreCase("ENABLED")
                || s.equals("1")
                || s.equalsIgnoreCase("Y")
                || s.equalsIgnoreCase("TRUE");
    }

    /** Vivance {@code user.user_id}: {@code INT} identity (not a column named {@code id}). */
    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    @Column(name = "user_id")
    private Integer id;

    @Column(name = "first_name", length = 45)
    private String firstName;

    @Column(name = "middle_name", length = 128)
    private String middleName;

    @Column(name = "last_name", length = 45)
    private String lastName;

    @Column(name = "email", nullable = false, length = 150)
    private String email;

    @Column(name = "password", nullable = false, length = 255)
    private String password;

    @Column(name = "status", nullable = false, length = 50)
    private String status;

    @Column(name = "created_on", updatable = false)
    private LocalDateTime createdOn;

    @Column(name = "modified_on")
    private LocalDateTime modifiedOn;

    @OneToMany(mappedBy = "user", cascade = CascadeType.ALL, fetch = FetchType.LAZY)
    @Builder.Default
    private List<Booking> bookings = new ArrayList<>();

    /** Combined name for APIs and TBO (built from {@code first_name} / {@code middle_name} / {@code last_name}). */
    public String getFullName() {
        return Stream.of(
                        firstName != null ? firstName.trim() : "",
                        middleName != null ? middleName.trim() : "",
                        lastName != null ? lastName.trim() : ""
                )
                .filter(s -> !s.isEmpty())
                .reduce((a, b) -> a + " " + b)
                .orElse("");
    }

    /** No role column in DB — hotel API always exposes {@code USER}. */
    public UserRole getRole() {
        return UserRole.USER;
    }

    /** Derived from {@link #status} for Spring Security. */
    public boolean isEnabled() {
        return isActiveStatus(status);
    }
}
