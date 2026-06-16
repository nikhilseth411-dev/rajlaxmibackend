package com.rajlaxmi.jewellers.service;

import com.rajlaxmi.jewellers.entity.AuditLog;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;

public interface AuditLogService {
    void log(Long userId, String userEmail, String action, String entityType, Long entityId,
             String description, String ipAddress);
    Page<AuditLog> getAdminLogs(Pageable pageable);
    Page<AuditLog> getLogsByUser(Long userId, Pageable pageable);
}
