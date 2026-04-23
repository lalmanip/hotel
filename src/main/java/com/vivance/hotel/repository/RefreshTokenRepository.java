package com.vivance.hotel.repository;

import com.vivance.hotel.domain.entity.RefreshToken;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;

import java.util.Optional;

public interface RefreshTokenRepository extends JpaRepository<RefreshToken, Long> {

    // Same query as vivance_api
    @Query(value = "SELECT * FROM refresh_tokens WHERE token_hash = ?1 AND revoked = 0 AND expires_at > NOW()", nativeQuery = true)
    Optional<RefreshToken> findActiveByTokenHash(String tokenHash);
}

