package com.exhibitorreg.auth;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.Mockito.times;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import com.exhibitorreg.common.AuditLogRepository;
import java.util.Optional;
import java.util.UUID;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.ArgumentCaptor;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.test.util.ReflectionTestUtils;

@ExtendWith(MockitoExtension.class)
class FailedLoginAttemptRecorderTest {

    @Mock
    private UserRepository userRepository;

    @Mock
    private AuditLogRepository auditLogRepository;

    private FailedLoginAttemptRecorder recorder;

    @BeforeEach
    void setUp() {
        recorder = new FailedLoginAttemptRecorder(userRepository, auditLogRepository);
    }

    private static User userWithAttempts(int attempts) {
        User user = new User();
        ReflectionTestUtils.setField(user, "id", UUID.randomUUID());
        user.setUsername("crew1");
        user.setFailedLoginAttempts(attempts);
        return user;
    }

    @Test
    void incrementsCounterWithoutLockingBelowThreshold() {
        User user = userWithAttempts(2);
        when(userRepository.findById(user.getId())).thenReturn(Optional.of(user));

        recorder.recordFailedAttempt(user.getId(), "127.0.0.1", "agent");

        assertThat(user.getFailedLoginAttempts()).isEqualTo(3);
        assertThat(user.isAccountLocked()).isFalse();
        verify(auditLogRepository, times(1)).save(org.mockito.ArgumentMatchers.any());
    }

    @Test
    void fifthAttemptLocksTheAccountAndWritesTwoAuditEntries() {
        User user = userWithAttempts(4);
        when(userRepository.findById(user.getId())).thenReturn(Optional.of(user));

        recorder.recordFailedAttempt(user.getId(), "127.0.0.1", "agent");

        assertThat(user.getFailedLoginAttempts()).isEqualTo(5);
        assertThat(user.isAccountLocked()).isTrue();
        assertThat(user.getLockedAt()).isNotNull();
        verify(auditLogRepository, times(2)).save(org.mockito.ArgumentMatchers.any());
    }

    @Test
    void unknownUserAttemptWritesAuditWithoutUser() {
        recorder.recordUnknownUserAttempt("ghost", "127.0.0.1", "agent");

        ArgumentCaptor<com.exhibitorreg.common.AuditLog> captor =
                ArgumentCaptor.forClass(com.exhibitorreg.common.AuditLog.class);
        verify(auditLogRepository).save(captor.capture());
        assertThat(captor.getValue().getUser()).isNull();
        assertThat(captor.getValue().getUsernameAttempted()).isEqualTo("ghost");
        assertThat(captor.getValue().getEventType()).isEqualTo(com.exhibitorreg.common.AuditEventType.LOGIN_FAILURE);
    }
}
