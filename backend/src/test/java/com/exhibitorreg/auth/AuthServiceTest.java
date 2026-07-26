package com.exhibitorreg.auth;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import com.exhibitorreg.auth.dto.ChangePasswordRequest;
import com.exhibitorreg.auth.dto.LoginRequest;
import com.exhibitorreg.auth.dto.LoginResponse;
import com.exhibitorreg.auth.dto.TokenPair;
import com.exhibitorreg.auth.dto.TotpLoginRequest;
import com.exhibitorreg.common.AuditLogRepository;
import com.exhibitorreg.common.exception.AccountLockedException;
import com.exhibitorreg.common.exception.InvalidCredentialsException;
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
class AuthServiceTest {

    @Mock
    private UserRepository userRepository;

    @Mock
    private AuditLogRepository auditLogRepository;

    @Mock
    private PasswordEncoder passwordEncoder;

    @Mock
    private JwtService jwtService;

    @Mock
    private RefreshTokenService refreshTokenService;

    @Mock
    private LoginTicketStore loginTicketStore;

    @Mock
    private TotpService totpService;

    @Mock
    private FailedLoginAttemptRecorder failedLoginAttemptRecorder;

    private AuthService authService;

    @BeforeEach
    void setUp() {
        authService = new AuthService(
                userRepository, auditLogRepository, passwordEncoder, jwtService, refreshTokenService,
                loginTicketStore, totpService, failedLoginAttemptRecorder);
    }

    private static User adminUser() {
        User user = new User();
        ReflectionTestUtils.setField(user, "id", UUID.randomUUID());
        user.setUsername("admin1");
        user.setPasswordHash("hashed");
        user.setRole(UserRole.ADMIN);
        return user;
    }

    private static User crewUser() {
        User user = new User();
        ReflectionTestUtils.setField(user, "id", UUID.randomUUID());
        user.setUsername("crew1");
        user.setPasswordHash("hashed");
        user.setRole(UserRole.CREW);
        user.setTotpSecret("SECRET");
        return user;
    }

    @Test
    void loginWithUnknownUsernameThrowsInvalidCredentialsAndRecordsAttempt() {
        when(userRepository.findByUsername("ghost")).thenReturn(Optional.empty());

        assertThatThrownBy(() -> authService.login(
                        new LoginRequest(UserRole.ADMIN, "ghost", "pw"), "127.0.0.1", "agent"))
                .isInstanceOf(InvalidCredentialsException.class);

        verify(failedLoginAttemptRecorder).recordUnknownUserAttempt("ghost", "127.0.0.1", "agent");
    }

    @Test
    void loginWithRoleMismatchThrowsInvalidCredentials() {
        when(userRepository.findByUsername("admin1")).thenReturn(Optional.of(adminUser()));

        assertThatThrownBy(() -> authService.login(
                        new LoginRequest(UserRole.ORGANISER, "admin1", "pw"), "127.0.0.1", "agent"))
                .isInstanceOf(InvalidCredentialsException.class);

        verify(passwordEncoder, never()).matches(anyString(), anyString());
    }

    @Test
    void loginWithLockedAccountThrowsAccountLocked() {
        User user = adminUser();
        user.setAccountLocked(true);
        when(userRepository.findByUsername("admin1")).thenReturn(Optional.of(user));

        assertThatThrownBy(() -> authService.login(
                        new LoginRequest(UserRole.ADMIN, "admin1", "pw"), "127.0.0.1", "agent"))
                .isInstanceOf(AccountLockedException.class);
    }

    @Test
    void loginWithWrongPasswordDelegatesToFailedAttemptRecorder() {
        User user = adminUser();
        when(userRepository.findByUsername("admin1")).thenReturn(Optional.of(user));
        when(passwordEncoder.matches("wrong", "hashed")).thenReturn(false);

        assertThatThrownBy(() -> authService.login(
                        new LoginRequest(UserRole.ADMIN, "admin1", "wrong"), "127.0.0.1", "agent"))
                .isInstanceOf(InvalidCredentialsException.class);

        // Recording happens in FailedLoginAttemptRecorder's own REQUIRES_NEW transaction so it
        // survives this method's rollback — see FailedLoginAttemptRecorderTest for the counting/
        // lockout logic itself.
        verify(failedLoginAttemptRecorder).recordFailedAttempt(user.getId(), "127.0.0.1", "agent");
    }

    @Test
    void successfulAdminLoginIssuesTokensImmediately() {
        User user = adminUser();
        when(userRepository.findByUsername("admin1")).thenReturn(Optional.of(user));
        when(passwordEncoder.matches("correct", "hashed")).thenReturn(true);
        when(jwtService.issueAccessToken(any())).thenReturn("access-token");
        when(refreshTokenService.issue(user.getId())).thenReturn("refresh-token");

        LoginResponse response =
                authService.login(new LoginRequest(UserRole.ADMIN, "admin1", "correct"), "127.0.0.1", "agent");

        assertThat(response.totpRequired()).isFalse();
        assertThat(response.tokens().accessToken()).isEqualTo("access-token");
        assertThat(response.tokens().refreshToken()).isEqualTo("refresh-token");
        verify(loginTicketStore, never()).issue(any());
    }

    @Test
    void successfulCrewLoginReturnsTotpRequiredWithoutIssuingTokens() {
        User user = crewUser();
        when(userRepository.findByUsername("crew1")).thenReturn(Optional.of(user));
        when(passwordEncoder.matches("correct", "hashed")).thenReturn(true);
        when(loginTicketStore.issue(user.getId())).thenReturn("ticket-123");

        LoginResponse response =
                authService.login(new LoginRequest(UserRole.CREW, "crew1", "correct"), "127.0.0.1", "agent");

        assertThat(response.totpRequired()).isTrue();
        assertThat(response.loginTicketId()).isEqualTo("ticket-123");
        assertThat(response.tokens()).isNull();
        verify(jwtService, never()).issueAccessToken(any());
    }

    @Test
    void totpLoginWithExpiredTicketThrows() {
        when(loginTicketStore.consume("bad-ticket")).thenReturn(Optional.empty());

        assertThatThrownBy(() -> authService.completeTotpLogin(
                        new TotpLoginRequest("bad-ticket", "123456"), "127.0.0.1", "agent"))
                .isInstanceOf(InvalidCredentialsException.class);
    }

    @Test
    void totpLoginWithWrongCodeDelegatesToFailedAttemptRecorder() {
        User user = crewUser();
        when(loginTicketStore.consume("ticket-123")).thenReturn(Optional.of(user.getId()));
        when(userRepository.findById(user.getId())).thenReturn(Optional.of(user));
        when(totpService.verify("SECRET", "000000")).thenReturn(false);

        assertThatThrownBy(() -> authService.completeTotpLogin(
                        new TotpLoginRequest("ticket-123", "000000"), "127.0.0.1", "agent"))
                .isInstanceOf(InvalidCredentialsException.class);

        verify(failedLoginAttemptRecorder).recordFailedAttempt(user.getId(), "127.0.0.1", "agent");
    }

    @Test
    void totpLoginSuccessIssuesTokens() {
        User user = crewUser();
        when(loginTicketStore.consume("ticket-123")).thenReturn(Optional.of(user.getId()));
        when(userRepository.findById(user.getId())).thenReturn(Optional.of(user));
        when(totpService.verify("SECRET", "654321")).thenReturn(true);
        when(jwtService.issueAccessToken(any())).thenReturn("access-token");
        when(refreshTokenService.issue(user.getId())).thenReturn("refresh-token");

        LoginResponse response =
                authService.completeTotpLogin(new TotpLoginRequest("ticket-123", "654321"), "127.0.0.1", "agent");

        assertThat(response.totpRequired()).isFalse();
        assertThat(response.tokens().accessToken()).isEqualTo("access-token");
    }

    @Test
    void refreshWithInvalidTokenThrows() {
        when(refreshTokenService.rotate("bad-token")).thenReturn(Optional.empty());

        assertThatThrownBy(() -> authService.refresh("bad-token")).isInstanceOf(InvalidCredentialsException.class);
    }

    @Test
    void refreshReChecksLockedStateFromDatabase() {
        User user = adminUser();
        user.setAccountLocked(true);
        when(refreshTokenService.rotate("old-token"))
                .thenReturn(Optional.of(new RefreshTokenService.Rotated("new-token", user.getId())));
        when(userRepository.findById(user.getId())).thenReturn(Optional.of(user));

        assertThatThrownBy(() -> authService.refresh("old-token")).isInstanceOf(AccountLockedException.class);
    }

    @Test
    void logoutRevokesTokenAndWritesAudit() {
        User user = adminUser();
        AuthenticatedPrincipal principal =
                new AuthenticatedPrincipal(user.getId(), user.getUsername(), user.getRole(), false);
        when(userRepository.findById(user.getId())).thenReturn(Optional.of(user));

        authService.logout("refresh-token", principal, "127.0.0.1", "agent");

        verify(refreshTokenService).revoke("refresh-token");
        verify(auditLogRepository).save(any());
    }

    @Test
    void changePasswordWithWrongCurrentPasswordThrows() {
        User user = adminUser();
        when(userRepository.findById(user.getId())).thenReturn(Optional.of(user));
        when(passwordEncoder.matches("wrong-current", "hashed")).thenReturn(false);

        AuthenticatedPrincipal principal =
                new AuthenticatedPrincipal(user.getId(), user.getUsername(), user.getRole(), true);

        assertThatThrownBy(() -> authService.changePassword(
                        principal, new ChangePasswordRequest("wrong-current", "newpassword1")))
                .isInstanceOf(InvalidCredentialsException.class);
    }

    @Test
    void changePasswordSuccessClearsFlagAndIssuesFreshTokens() {
        User user = adminUser();
        user.setMustChangePassword(true);
        when(userRepository.findById(user.getId())).thenReturn(Optional.of(user));
        when(passwordEncoder.matches("correct-current", "hashed")).thenReturn(true);
        when(passwordEncoder.encode("newpassword1")).thenReturn("new-hashed");
        when(jwtService.issueAccessToken(any())).thenReturn("access-token");
        when(refreshTokenService.issue(user.getId())).thenReturn("refresh-token");

        AuthenticatedPrincipal principal =
                new AuthenticatedPrincipal(user.getId(), user.getUsername(), user.getRole(), true);

        TokenPair result =
                authService.changePassword(principal, new ChangePasswordRequest("correct-current", "newpassword1"));

        assertThat(user.isMustChangePassword()).isFalse();
        assertThat(user.getPasswordHash()).isEqualTo("new-hashed");
        assertThat(result.accessToken()).isEqualTo("access-token");
    }
}
