package com.exhibitorreg.auth;

import com.exhibitorreg.common.AuditEventType;
import com.exhibitorreg.common.AuditLog;
import com.exhibitorreg.common.AuditLogRepository;
import com.exhibitorreg.common.exception.NotFoundException;
import java.time.Instant;
import java.util.UUID;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Propagation;
import org.springframework.transaction.annotation.Transactional;

/**
 * Records failed-login bookkeeping (attempt counter, lockout, audit log) in its own
 * {@code REQUIRES_NEW} transaction, deliberately separate from the caller's transaction.
 * AuthService's login methods throw an exception on every failure path to signal the
 * caller — and Spring rolls back the whole enclosing @Transactional method on a thrown
 * RuntimeException, which would silently undo the very counter/lockout writes this class
 * exists to make durable. Kept in a separate bean (not a private method on AuthService)
 * because REQUIRES_NEW only takes effect through the Spring proxy — self-invocation
 * within the same class bypasses it entirely.
 */
@Service
public class FailedLoginAttemptRecorder {

    private static final int MAX_FAILED_ATTEMPTS = 5;

    private final UserRepository userRepository;
    private final AuditLogRepository auditLogRepository;

    public FailedLoginAttemptRecorder(UserRepository userRepository, AuditLogRepository auditLogRepository) {
        this.userRepository = userRepository;
        this.auditLogRepository = auditLogRepository;
    }

    @Transactional(propagation = Propagation.REQUIRES_NEW)
    public void recordFailedAttempt(UUID userId, String ipAddress, String userAgent) {
        User user = userRepository.findById(userId)
                .orElseThrow(() -> new NotFoundException("User not found: " + userId));

        int attempts = user.getFailedLoginAttempts() + 1;
        user.setFailedLoginAttempts(attempts);
        boolean justLocked = attempts >= MAX_FAILED_ATTEMPTS;
        if (justLocked) {
            user.setAccountLocked(true);
            user.setLockedAt(Instant.now());
        }
        userRepository.save(user);

        writeAudit(user, user.getUsername(), AuditEventType.LOGIN_FAILURE, ipAddress, userAgent);
        if (justLocked) {
            writeAudit(user, user.getUsername(), AuditEventType.ACCOUNT_LOCKED, ipAddress, userAgent);
        }
    }

    @Transactional(propagation = Propagation.REQUIRES_NEW)
    public void recordUnknownUserAttempt(String usernameAttempted, String ipAddress, String userAgent) {
        writeAudit(null, usernameAttempted, AuditEventType.LOGIN_FAILURE, ipAddress, userAgent);
    }

    private void writeAudit(User user, String usernameAttempted, AuditEventType eventType, String ipAddress, String userAgent) {
        AuditLog log = new AuditLog();
        log.setUser(user);
        log.setUsernameAttempted(usernameAttempted);
        log.setEventType(eventType);
        log.setIpAddress(ipAddress);
        log.setUserAgent(userAgent);
        auditLogRepository.save(log);
    }
}
