package com.exhibitorreg.admin;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import com.exhibitorreg.admin.dto.CreateCrewOrValidatorRequest;
import com.exhibitorreg.admin.dto.CreateOrganiserRequest;
import com.exhibitorreg.admin.dto.CreatedActorResponse;
import com.exhibitorreg.auth.TotpService;
import com.exhibitorreg.auth.User;
import com.exhibitorreg.auth.UserRepository;
import com.exhibitorreg.auth.UserRole;
import com.exhibitorreg.common.AuditLogRepository;
import com.exhibitorreg.common.exception.BusinessRuleViolationException;
import com.exhibitorreg.common.exception.ConflictException;
import com.exhibitorreg.common.exception.NotFoundException;
import java.util.Optional;
import java.util.UUID;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.test.util.ReflectionTestUtils;

@ExtendWith(MockitoExtension.class)
class AdminActorServiceTest {

    @Mock
    private UserRepository userRepository;

    @Mock
    private AuditLogRepository auditLogRepository;

    @Mock
    private PasswordEncoder passwordEncoder;

    @Mock
    private TotpService totpService;

    private AdminActorService service;

    @BeforeEach
    void setUp() {
        service = new AdminActorService(userRepository, auditLogRepository, passwordEncoder, totpService, true);
    }

    private static User userWithId(UserRole role) {
        User user = new User();
        ReflectionTestUtils.setField(user, "id", UUID.randomUUID());
        user.setRole(role);
        return user;
    }

    @Test
    void createOrganiserRejectsDuplicateUsername() {
        when(userRepository.findByUsername("org1")).thenReturn(Optional.of(userWithId(UserRole.ORGANISER)));

        assertThatThrownBy(() -> service.createOrganiser(
                        new CreateOrganiserRequest("org1", "org1@example.com", "password1")))
                .isInstanceOf(ConflictException.class);
    }

    @Test
    void createOrganiserHasNoTotpAndMustChangePassword() {
        when(userRepository.findByUsername("org1")).thenReturn(Optional.empty());
        when(passwordEncoder.encode("password1")).thenReturn("hashed");

        CreatedActorResponse response =
                service.createOrganiser(new CreateOrganiserRequest("org1", "org1@example.com", "password1"));

        assertThat(response.totpQrPngBase64()).isNull();
        verify(totpService, org.mockito.Mockito.never()).generateSecret();
    }

    @Test
    void createCrewGeneratesTotpSecretAndQr() {
        when(userRepository.findByUsername("crew1")).thenReturn(Optional.empty());
        when(passwordEncoder.encode("password1")).thenReturn("hashed");
        when(totpService.generateSecret()).thenReturn("SECRET123");
        when(totpService.buildQrPngBase64("crew1", "SECRET123")).thenReturn("base64png");

        CreatedActorResponse response = service.createCrew(
                new CreateCrewOrValidatorRequest("crew1", "password1", "123456789012", "9876543210"));

        assertThat(response.totpQrPngBase64()).isEqualTo("base64png");
    }

    @Test
    void unlockClearsLockStateAndWritesAudit() {
        User user = userWithId(UserRole.CREW);
        user.setAccountLocked(true);
        user.setFailedLoginAttempts(5);
        when(userRepository.findById(user.getId())).thenReturn(Optional.of(user));

        service.unlock(user.getId());

        assertThat(user.isAccountLocked()).isFalse();
        assertThat(user.getFailedLoginAttempts()).isZero();
        assertThat(user.getLockedAt()).isNull();
        verify(auditLogRepository).save(any());
    }

    @Test
    void unlockUnknownActorThrowsNotFound() {
        UUID id = UUID.randomUUID();
        when(userRepository.findById(id)).thenReturn(Optional.empty());

        assertThatThrownBy(() -> service.unlock(id)).isInstanceOf(NotFoundException.class);
    }

    @Test
    void regenerateTotpRejectedForNonTotpRoles() {
        User admin = userWithId(UserRole.ADMIN);
        when(userRepository.findById(admin.getId())).thenReturn(Optional.of(admin));

        assertThatThrownBy(() -> service.regenerateTotpQr(admin.getId()))
                .isInstanceOf(BusinessRuleViolationException.class);
    }

    @Test
    void regenerateTotpReplacesSecretForValidator() {
        User validator = userWithId(UserRole.VALIDATOR);
        validator.setUsername("validator1");
        when(userRepository.findById(validator.getId())).thenReturn(Optional.of(validator));
        when(totpService.generateSecret()).thenReturn("NEWSECRET");
        when(totpService.buildQrPngBase64("validator1", "NEWSECRET")).thenReturn("newqr");

        var response = service.regenerateTotpQr(validator.getId());

        assertThat(validator.getTotpSecret()).isEqualTo("NEWSECRET");
        assertThat(response.totpQrPngBase64()).isEqualTo("newqr");
    }

    @Test
    void setActiveTogglesFlag() {
        User user = userWithId(UserRole.ORGANISER);
        user.setActive(true);
        when(userRepository.findById(user.getId())).thenReturn(Optional.of(user));

        service.setActive(user.getId(), false);

        assertThat(user.isActive()).isFalse();
    }
}
