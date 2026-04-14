package com.vivance.hotel.repository;

import com.vivance.hotel.domain.entity.TboAuthToken;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.time.LocalDate;
import java.util.Optional;

@Repository
public interface TboAuthTokenRepository extends JpaRepository<TboAuthToken, Long> {

    Optional<TboAuthToken> findByTokenDate(LocalDate tokenDate);
}
