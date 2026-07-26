package com.exhibitorreg.admin;

import com.exhibitorreg.admin.dto.AuditLogSummary;
import com.exhibitorreg.common.AuditEventType;
import com.exhibitorreg.common.AuditLog;
import com.exhibitorreg.common.AuditLogRepository;
import java.time.Instant;
import java.util.UUID;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.domain.Specification;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Service
public class AdminAuditService {

    private final AuditLogRepository auditLogRepository;

    public AdminAuditService(AuditLogRepository auditLogRepository) {
        this.auditLogRepository = auditLogRepository;
    }

    @Transactional(readOnly = true)
    public Page<AuditLogSummary> search(
            UUID userId, AuditEventType eventType, Instant from, Instant to, Pageable pageable) {
        Specification<AuditLog> spec = Specification.unrestricted();

        if (userId != null) {
            spec = spec.and((root, query, cb) -> cb.equal(root.get("user").get("id"), userId));
        }
        if (eventType != null) {
            spec = spec.and((root, query, cb) -> cb.equal(root.get("eventType"), eventType));
        }
        if (from != null) {
            spec = spec.and((root, query, cb) -> cb.greaterThanOrEqualTo(root.get("createdAt"), from));
        }
        if (to != null) {
            spec = spec.and((root, query, cb) -> cb.lessThanOrEqualTo(root.get("createdAt"), to));
        }

        return auditLogRepository.findAll(spec, pageable).map(AuditLogSummary::from);
    }
}
