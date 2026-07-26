package com.exhibitorreg.admin;

import static org.assertj.core.api.Assertions.assertThat;

import com.exhibitorreg.auth.User;
import com.exhibitorreg.auth.UserRepository;
import com.exhibitorreg.auth.UserRole;
import com.exhibitorreg.common.AuditEventType;
import com.exhibitorreg.common.AuditLog;
import com.exhibitorreg.common.AuditLogRepository;
import com.exhibitorreg.config.JpaAuditingConfig;
import java.time.Instant;
import java.time.temporal.ChronoUnit;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.data.jpa.test.autoconfigure.DataJpaTest;
import org.springframework.boot.jdbc.test.autoconfigure.AutoConfigureTestDatabase;
import org.springframework.context.annotation.Import;
import org.springframework.data.domain.PageRequest;

@DataJpaTest
@AutoConfigureTestDatabase(replace = AutoConfigureTestDatabase.Replace.NONE)
@Import(JpaAuditingConfig.class)
class AdminAuditServiceDataTest {

    @Autowired
    private UserRepository userRepository;

    @Autowired
    private AuditLogRepository auditLogRepository;

    @Test
    void filtersByUserIdAndEventTypeAndTimeRange() {
        User user = new User();
        user.setUsername("admin1");
        user.setPasswordHash("hashed");
        user.setRole(UserRole.ADMIN);
        userRepository.saveAndFlush(user);

        User otherUser = new User();
        otherUser.setUsername("admin2");
        otherUser.setPasswordHash("hashed");
        otherUser.setRole(UserRole.ADMIN);
        userRepository.saveAndFlush(otherUser);

        saveLog(user, AuditEventType.LOGIN_SUCCESS);
        saveLog(user, AuditEventType.LOGOUT);
        saveLog(otherUser, AuditEventType.LOGIN_SUCCESS);

        AdminAuditService service = new AdminAuditService(auditLogRepository);

        var byUser = service.search(user.getId(), null, null, null, PageRequest.of(0, 10));
        assertThat(byUser.getTotalElements()).isEqualTo(2);

        var byUserAndEventType =
                service.search(user.getId(), AuditEventType.LOGIN_SUCCESS, null, null, PageRequest.of(0, 10));
        assertThat(byUserAndEventType.getTotalElements()).isEqualTo(1);

        var futureOnly = service.search(
                null, null, Instant.now().plus(1, ChronoUnit.DAYS), null, PageRequest.of(0, 10));
        assertThat(futureOnly.getTotalElements()).isZero();
    }

    private void saveLog(User user, AuditEventType eventType) {
        AuditLog log = new AuditLog();
        log.setUser(user);
        log.setUsernameAttempted(user.getUsername());
        log.setEventType(eventType);
        auditLogRepository.saveAndFlush(log);
    }
}
