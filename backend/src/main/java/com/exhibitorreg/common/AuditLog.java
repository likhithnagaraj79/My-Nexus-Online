package com.exhibitorreg.common;

import com.exhibitorreg.auth.User;
import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.EnumType;
import jakarta.persistence.Enumerated;
import jakarta.persistence.JoinColumn;
import jakarta.persistence.ManyToOne;
import jakarta.persistence.Table;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

@Getter
@Setter
@NoArgsConstructor
@Entity
@Table(name = "audit_logs")
public class AuditLog extends Auditable {

    // ON DELETE SET NULL is defined in V2__create_audit_logs_table.sql (Flyway owns the schema).
    @ManyToOne
    @JoinColumn(name = "user_id")
    private User user;

    @Column(name = "username_attempted", length = 100)
    private String usernameAttempted;

    @Enumerated(EnumType.STRING)
    @Column(name = "event_type", nullable = false, length = 30)
    private AuditEventType eventType;

    @Column(name = "ip_address", length = 45)
    private String ipAddress;

    @Column(name = "user_agent", length = 255)
    private String userAgent;
}
