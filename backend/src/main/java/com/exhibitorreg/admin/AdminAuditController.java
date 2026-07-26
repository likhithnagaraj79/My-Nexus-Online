package com.exhibitorreg.admin;

import com.exhibitorreg.admin.dto.AuditLogSummary;
import com.exhibitorreg.common.AuditEventType;
import java.time.Instant;
import java.util.UUID;
import org.springframework.data.domain.Pageable;
import org.springframework.data.web.PagedModel;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequestMapping("/api/admin/audit-logs")
public class AdminAuditController {

    private final AdminAuditService adminAuditService;

    public AdminAuditController(AdminAuditService adminAuditService) {
        this.adminAuditService = adminAuditService;
    }

    @GetMapping
    public PagedModel<AuditLogSummary> search(
            @RequestParam(required = false) UUID userId,
            @RequestParam(required = false) AuditEventType eventType,
            @RequestParam(required = false) Instant from,
            @RequestParam(required = false) Instant to,
            Pageable pageable) {
        return new PagedModel<>(adminAuditService.search(userId, eventType, from, to, pageable));
    }
}
