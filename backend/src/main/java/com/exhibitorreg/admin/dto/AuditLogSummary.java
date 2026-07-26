package com.exhibitorreg.admin.dto;

import com.exhibitorreg.common.AuditEventType;
import com.exhibitorreg.common.AuditLog;
import java.time.Instant;
import java.util.UUID;

public record AuditLogSummary(
        UUID id,
        UUID userId,
        String usernameAttempted,
        AuditEventType eventType,
        String ipAddress,
        String userAgent,
        Instant occurredAt) {

    public static AuditLogSummary from(AuditLog log) {
        return new AuditLogSummary(
                log.getId(),
                log.getUser() != null ? log.getUser().getId() : null,
                log.getUsernameAttempted(),
                log.getEventType(),
                log.getIpAddress(),
                log.getUserAgent(),
                log.getCreatedAt());
    }
}
