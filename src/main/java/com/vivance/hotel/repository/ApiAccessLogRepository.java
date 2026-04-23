package com.vivance.hotel.repository;

import com.vivance.hotel.domain.entity.ApiAccessLog;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

@Repository
public interface ApiAccessLogRepository extends JpaRepository<ApiAccessLog, Long> {
}
