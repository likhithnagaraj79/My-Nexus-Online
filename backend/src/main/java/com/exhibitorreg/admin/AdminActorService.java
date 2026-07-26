package com.exhibitorreg.admin;

import com.exhibitorreg.admin.dto.ActorSummary;
import com.exhibitorreg.admin.dto.CreateCrewOrValidatorRequest;
import com.exhibitorreg.admin.dto.CreateOrganiserRequest;
import com.exhibitorreg.admin.dto.CreatedActorResponse;
import com.exhibitorreg.admin.dto.TotpQrResponse;
import com.exhibitorreg.auth.TotpService;
import com.exhibitorreg.auth.User;
import com.exhibitorreg.auth.UserRepository;
import com.exhibitorreg.auth.UserRole;
import com.exhibitorreg.common.AuditEventType;
import com.exhibitorreg.common.AuditLog;
import com.exhibitorreg.common.AuditLogRepository;
import com.exhibitorreg.common.exception.BusinessRuleViolationException;
import com.exhibitorreg.common.exception.ConflictException;
import com.exhibitorreg.common.exception.NotFoundException;
import java.util.UUID;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.domain.Specification;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Service
public class AdminActorService {

    private final UserRepository userRepository;
    private final AuditLogRepository auditLogRepository;
    private final PasswordEncoder passwordEncoder;
    private final TotpService totpService;
    private final boolean totpEnabled;

    public AdminActorService(
            UserRepository userRepository,
            AuditLogRepository auditLogRepository,
            PasswordEncoder passwordEncoder,
            TotpService totpService,
            @Value("${app.totp.enabled}") boolean totpEnabled) {
        this.userRepository = userRepository;
        this.auditLogRepository = auditLogRepository;
        this.passwordEncoder = passwordEncoder;
        this.totpService = totpService;
        this.totpEnabled = totpEnabled;
    }

    @Transactional
    public CreatedActorResponse createOrganiser(CreateOrganiserRequest request) {
        ensureUsernameAvailable(request.username());

        User user = new User();
        user.setUsername(request.username());
        user.setEmail(request.email());
        user.setPasswordHash(passwordEncoder.encode(request.temporaryPassword()));
        user.setRole(UserRole.ORGANISER);
        user.setMustChangePassword(true);
        userRepository.save(user);

        return new CreatedActorResponse(user.getId(), null);
    }

    @Transactional
    public CreatedActorResponse createCrew(CreateCrewOrValidatorRequest request) {
        return createCrewOrValidator(request, UserRole.CREW);
    }

    @Transactional
    public CreatedActorResponse createValidator(CreateCrewOrValidatorRequest request) {
        return createCrewOrValidator(request, UserRole.VALIDATOR);
    }

    private CreatedActorResponse createCrewOrValidator(CreateCrewOrValidatorRequest request, UserRole role) {
        ensureUsernameAvailable(request.username());

        String secret = totpEnabled ? totpService.generateSecret() : null;

        User user = new User();
        user.setUsername(request.username());
        user.setPasswordHash(passwordEncoder.encode(request.temporaryPassword()));
        user.setRole(role);
        user.setAadharNumber(request.aadharNumber());
        user.setPhoneNumber(request.phoneNumber());
        user.setTotpSecret(secret);
        user.setMustChangePassword(true);
        userRepository.save(user);

        String qrPngBase64 = totpEnabled ? totpService.buildQrPngBase64(user.getUsername(), secret) : null;
        return new CreatedActorResponse(user.getId(), qrPngBase64);
    }

    @Transactional(readOnly = true)
    public Page<ActorSummary> listActors(UserRole role, Boolean active, Pageable pageable) {
        Specification<User> spec = Specification.unrestricted();
        if (role != null) {
            spec = spec.and((root, query, cb) -> cb.equal(root.get("role"), role));
        }
        if (active != null) {
            spec = spec.and((root, query, cb) -> cb.equal(root.get("active"), active));
        }
        return userRepository.findAll(spec, pageable).map(ActorSummary::from);
    }

    @Transactional
    public void unlock(UUID id) {
        User user = getOrThrow(id);
        user.setAccountLocked(false);
        user.setFailedLoginAttempts(0);
        user.setLockedAt(null);
        userRepository.save(user);

        AuditLog log = new AuditLog();
        log.setUser(user);
        log.setUsernameAttempted(user.getUsername());
        log.setEventType(AuditEventType.ACCOUNT_UNLOCKED);
        auditLogRepository.save(log);
    }

    @Transactional
    public void setActive(UUID id, boolean active) {
        User user = getOrThrow(id);
        user.setActive(active);
        userRepository.save(user);
    }

    @Transactional
    public TotpQrResponse regenerateTotpQr(UUID id) {
        User user = getOrThrow(id);
        if (user.getRole() != UserRole.CREW && user.getRole() != UserRole.VALIDATOR) {
            throw new BusinessRuleViolationException("Only Crew and Validator accounts use TOTP.");
        }

        String secret = totpService.generateSecret();
        user.setTotpSecret(secret);
        userRepository.save(user);

        return new TotpQrResponse(totpService.buildQrPngBase64(user.getUsername(), secret));
    }

    private User getOrThrow(UUID id) {
        return userRepository.findById(id).orElseThrow(() -> new NotFoundException("Actor not found: " + id));
    }

    private void ensureUsernameAvailable(String username) {
        if (userRepository.findByUsername(username).isPresent()) {
            throw new ConflictException("Username '" + username + "' is already taken.");
        }
    }
}
