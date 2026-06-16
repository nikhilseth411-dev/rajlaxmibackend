package com.rajlaxmi.jewellers.service.impl;

import com.rajlaxmi.jewellers.entity.AuditLog;
import com.rajlaxmi.jewellers.repository.AuditLogRepository;
import com.rajlaxmi.jewellers.service.AuditLogService;
import lombok.RequiredArgsConstructor;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.scheduling.annotation.Async;
import org.springframework.stereotype.Service;

import java.time.LocalDateTime;

@Service
@RequiredArgsConstructor
public class AuditLogServiceImpl implements AuditLogService {

    private final AuditLogRepository auditLogRepository;

    @Override
    @Async
    public void log(Long userId, String userEmail, String action, String entityType,
                    Long entityId, String description, String ipAddress) {
        try {
            auditLogRepository.save(AuditLog.builder()
                    .userId(userId)
                    .userEmail(userEmail)
                    .action(action)
                    .entityType(entityType)
                    .entityId(entityId)
                    .description(description)
                    .ipAddress(ipAddress)
                    .createdAt(LocalDateTime.now())
                    .build());
        } catch (Exception ignored) {
            // Audit log failure must never break the main operation
        }
    }

    @Override
    public Page<AuditLog> getAdminLogs(Pageable pageable) {
        return auditLogRepository.findAllByOrderByCreatedAtDesc(pageable);
    }

    @Override
    public Page<AuditLog> getLogsByUser(Long userId, Pageable pageable) {
        return auditLogRepository.findByUserIdOrderByCreatedAtDesc(userId, pageable);
    }
}
