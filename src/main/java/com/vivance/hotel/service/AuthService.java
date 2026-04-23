package com.vivance.hotel.service;

import com.vivance.hotel.domain.entity.User;
import com.vivance.hotel.dto.request.LoginRequest;
import com.vivance.hotel.dto.request.RegisterRequest;
import com.vivance.hotel.dto.response.AuthResponse;
import com.vivance.hotel.exception.ResourceNotFoundException;
import com.vivance.hotel.repository.UserRepository;
import com.vivance.hotel.security.JwtTokenProvider;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.security.authentication.AuthenticationManager;
import org.springframework.security.authentication.UsernamePasswordAuthenticationToken;
import org.springframework.security.core.Authentication;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDateTime;

@Slf4j
@Service
@RequiredArgsConstructor
public class AuthService {

    /** Written to {@code user.status} for new registrations — adjust if your schema uses another active value. */
    private static final String DEFAULT_USER_STATUS = "ACTIVE";

    private final UserRepository userRepository;
    private final PasswordEncoder passwordEncoder;
    private final AuthenticationManager authenticationManager;
    private final JwtTokenProvider jwtTokenProvider;

    @Transactional
    public AuthResponse register(RegisterRequest request) {
        if (userRepository.existsByEmail(request.getEmail())) {
            throw new IllegalArgumentException("Email is already registered: " + request.getEmail());
        }

        String[] nameParts = splitFullName(request.getFullName());
        LocalDateTime now = LocalDateTime.now();
        User user = User.builder()
                .firstName(nameParts[0])
                .lastName(nameParts[1])
                .email(request.getEmail())
                .password(passwordEncoder.encode(request.getPassword()))
                .status(DEFAULT_USER_STATUS)
                .createdOn(now)
                .modifiedOn(now)
                .build();

        user = userRepository.save(user);
        log.info("New user registered: {}", user.getEmail());

        String token = jwtTokenProvider.generateToken(user.getEmail());
        return buildAuthResponse(user, token);
    }

    public AuthResponse login(LoginRequest request) {
        Authentication authentication = authenticationManager.authenticate(
                new UsernamePasswordAuthenticationToken(request.getEmail(), request.getPassword())
        );

        String token = jwtTokenProvider.generateToken(authentication);

        User user = userRepository.findByEmail(request.getEmail())
                .orElseThrow(() -> new ResourceNotFoundException("User", "email", request.getEmail()));

        log.info("User logged in: {}", user.getEmail());
        return buildAuthResponse(user, token);
    }

    private AuthResponse buildAuthResponse(User user, String token) {
        return AuthResponse.builder()
                .accessToken(token)
                .tokenType("Bearer")
                .expiresIn(jwtTokenProvider.getExpirationMs() / 1000)
                .userId(user.getId().longValue())
                .email(user.getEmail())
                .fullName(user.getFullName())
                .role(user.getRole().name())
                .build();
    }

    /** Splits "First Last" → [first, last]. Single token → [token, "."]. */
    private static String[] splitFullName(String fullName) {
        if (fullName == null || fullName.isBlank()) {
            return new String[]{"Guest", "."};
        }
        String trimmed = fullName.trim();
        int space = trimmed.lastIndexOf(' ');
        if (space < 0) {
            return new String[]{trimmed, "."};
        }
        return new String[]{trimmed.substring(0, space).trim(), trimmed.substring(space + 1).trim()};
    }
}
