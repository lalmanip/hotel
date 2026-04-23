package com.vivance.hotel.service;

import com.vivance.hotel.domain.entity.ApiAccessLog;
import com.vivance.hotel.domain.entity.ApiCallEventLog;
import com.vivance.hotel.repository.ApiAccessLogRepository;
import com.vivance.hotel.repository.ApiCallEventLogRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Propagation;
import org.springframework.transaction.annotation.Transactional;

@Service
@RequiredArgsConstructor
public class ApiEventLogService {

    private final ApiCallEventLogRepository apiCallEventLogRepository;
    private final ApiAccessLogRepository apiAccessLogRepository;

    // REQUIRES_NEW suspends any surrounding read-only transaction so the INSERT always succeeds
    @Transactional(propagation = Propagation.REQUIRES_NEW)
    public ApiCallEventLog saveOrUpdateApiCallEvent(ApiCallEventLog entry) {
        if (entry != null) {
            apiCallEventLogRepository.save(entry);
        }
        return entry;
    }

    @Transactional(propagation = Propagation.REQUIRES_NEW)
    public void updateApiCallEventContent(Long id, String content) {
        if (id == null) return;
        apiCallEventLogRepository.findById(id).ifPresent(existing -> {
            existing.setContent(content);
            apiCallEventLogRepository.save(existing);
        });
    }

    @Transactional(propagation = Propagation.REQUIRES_NEW)
    public ApiAccessLog saveApiAccessLog(ApiAccessLog entry) {
        if (entry != null) {
            return apiAccessLogRepository.save(entry);
        }
        return entry;
    }
}
