package com.exhibitorreg.auth;

import com.exhibitorreg.auth.dto.ChangePasswordRequest;
import com.exhibitorreg.auth.dto.LoginRequest;
import com.exhibitorreg.auth.dto.LoginResponse;
import com.exhibitorreg.auth.dto.TokenPair;
import com.exhibitorreg.auth.dto.TotpLoginRequest;
import com.exhibitorreg.common.AuditEventType;
import com.exhibitorreg.common.AuditLog;
import com.exhibitorreg.common.AuditLogRepository;
import com.exhibitorreg.common.exception.AccountLockedException;
import com.exhibitorreg.common.exception.InvalidCredentialsException;
import java.util.UUID;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Service
public class AuthService {

    private final UserRepository userRepository;
    private final AuditLogRepository auditLogRepository;
    private final PasswordEncoder passwordEncoder;
    private final JwtService jwtService;
    private final RefreshTokenService refreshTokenService;
    private final LoginTicketStore loginTicketStore;
    private final TotpService totpService;
    private final FailedLoginAttemptRecorder failedLoginAttemptRecorder;

    public AuthService(
            UserRepository userRepository,
            AuditLogRepository auditLogRepository,
            PasswordEncoder passwordEncoder,
            JwtService jwtService,
            RefreshTokenService refreshTokenService,
            LoginTicketStore loginTicketStore,
            TotpService totpService,
            FailedLoginAttemptRecorder failedLoginAttemptRecorder) {
        this.userRepository = userRepository;
        this.auditLogRepository = auditLogRepository;
        this.passwordEncoder = passwordEncoder;
        this.jwtService = jwtService;
        this.refreshTokenService = refreshTokenService;
        this.loginTicketStore = loginTicketStore;
        this.totpService = totpService;
        this.failedLoginAttemptRecorder = failedLoginAttemptRecorder;
    }

    @Transactional
    public LoginResponse login(LoginRequest request, String ipAddress, String userAgent) {
        var maybeUser = userRepository.findByUsername(request.username());
        if (maybeUser.isEmpty() || maybeUser.get().getRole() != request.role() || !maybeUser.get().isActive()) {
            failedLoginAttemptRecorder.recordUnknownUserAttempt(request.username(), ipAddress, userAgent);
            throw new InvalidCredentialsException("Invalid username or password.");
        }

        User user = maybeUser.get();
        if (user.isAccountLocked()) {
            throw new AccountLockedException("This account is locked. Contact an administrator.");
        }

        if (!passwordEncoder.matches(request.password(), user.getPasswordHash())) {
            failedLoginAttemptRecorder.recordFailedAttempt(user.getId(), ipAddress, userAgent);
            throw new InvalidCredentialsException("Invalid username or password.");
        }

        user.setFailedLoginAttempts(0);
        userRepository.save(user);

        if (user.getRole() == UserRole.CREW || user.getRole() == UserRole.VALIDATOR) {
            return LoginResponse.totpRequired(loginTicketStore.issue(user.getId()));
        }

        writeAudit(user, user.getUsername(), AuditEventType.LOGIN_SUCCESS, ipAddress, userAgent);
        return LoginResponse.authenticated(issueTokens(user));
    }

    @Transactional
    public LoginResponse completeTotpLogin(TotpLoginRequest request, String ipAddress, String userAgent) {
        UUID userId = loginTicketStore.consume(request.loginTicketId())
                .orElseThrow(() -> new InvalidCredentialsException("Login session expired. Please log in again."));
        User user = userRepository.findById(userId)
                .orElseThrow(() -> new InvalidCredentialsException("Invalid login session."));

        if (user.isAccountLocked()) {
            throw new AccountLockedException("This account is locked. Contact an administrator.");
        }

        if (!totpService.verify(user.getTotpSecret(), request.code())) {
            failedLoginAttemptRecorder.recordFailedAttempt(user.getId(), ipAddress, userAgent);
            throw new InvalidCredentialsException("Invalid authentication code.");
        }

        user.setFailedLoginAttempts(0);
        userRepository.save(user);
        writeAudit(user, user.getUsername(), AuditEventType.LOGIN_SUCCESS, ipAddress, userAgent);
        return LoginResponse.authenticated(issueTokens(user));
    }

    @Transactional
    public TokenPair refresh(String refreshToken) {
        RefreshTokenService.Rotated rotated = refreshTokenService.rotate(refreshToken)
                .orElseThrow(() -> new InvalidCredentialsException("Invalid or expired refresh token."));
        User user = userRepository.findById(rotated.userId())
                .orElseThrow(() -> new InvalidCredentialsException("Invalid refresh token."));

        if (!user.isActive()) {
            throw new InvalidCredentialsException("Invalid refresh token.");
        }
        if (user.isAccountLocked()) {
            throw new AccountLockedException("This account is locked. Contact an administrator.");
        }

        return new TokenPair(
                jwtService.issueAccessToken(toPrincipal(user)), rotated.refreshToken(), user.isMustChangePassword());
    }

    @Transactional
    public void logout(String refreshToken, AuthenticatedPrincipal principal, String ipAddress, String userAgent) {
        refreshTokenService.revoke(refreshToken);
        userRepository.findById(principal.userId())
                .ifPresent(user -> writeAudit(user, user.getUsername(), AuditEventType.LOGOUT, ipAddress, userAgent));
    }

    @Transactional
    public TokenPair changePassword(AuthenticatedPrincipal principal, ChangePasswordRequest request) {
        User user = userRepository.findById(principal.userId())
                .orElseThrow(() -> new InvalidCredentialsException("Invalid session."));

        if (!passwordEncoder.matches(request.currentPassword(), user.getPasswordHash())) {
            throw new InvalidCredentialsException("Current password is incorrect.");
        }

        user.setPasswordHash(passwordEncoder.encode(request.newPassword()));
        user.setMustChangePassword(false);
        userRepository.save(user);
        return issueTokens(user);
    }

    private TokenPair issueTokens(User user) {
        String accessToken = jwtService.issueAccessToken(toPrincipal(user));
        String refreshToken = refreshTokenService.issue(user.getId());
        return new TokenPair(accessToken, refreshToken, user.isMustChangePassword());
    }

    private AuthenticatedPrincipal toPrincipal(User user) {
        return new AuthenticatedPrincipal(user.getId(), user.getUsername(), user.getRole(), user.isMustChangePassword());
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
