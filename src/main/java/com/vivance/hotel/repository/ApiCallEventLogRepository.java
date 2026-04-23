package com.vivance.hotel.repository;

import com.vivance.hotel.domain.entity.ApiCallEventLog;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

@Repository
public interface ApiCallEventLogRepository extends JpaRepository<ApiCallEventLog, Long> {
}
