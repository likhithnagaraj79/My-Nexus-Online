package com.exhibitorreg.auth;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.boot.CommandLineRunner;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Component;

/**
 * Creates the very first Admin account from env-provided credentials, only when the
 * users table is empty. Idempotent by construction — never overwrites or duplicates.
 */
@Component
public class RootAdminBootstrapRunner implements CommandLineRunner {

    private static final Logger log = LoggerFactory.getLogger(RootAdminBootstrapRunner.class);

    private final UserRepository userRepository;
    private final PasswordEncoder passwordEncoder;
    private final String bootstrapUsername;
    private final String bootstrapPassword;

    public RootAdminBootstrapRunner(
            UserRepository userRepository,
            PasswordEncoder passwordEncoder,
            @Value("${app.admin-bootstrap.username:}") String bootstrapUsername,
            @Value("${app.admin-bootstrap.password:}") String bootstrapPassword) {
        this.userRepository = userRepository;
        this.passwordEncoder = passwordEncoder;
        this.bootstrapUsername = bootstrapUsername;
        this.bootstrapPassword = bootstrapPassword;
    }

    @Override
    public void run(String... args) {
        if (userRepository.count() > 0) {
            return;
        }

        if (bootstrapUsername.isBlank() || bootstrapPassword.isBlank()) {
            log.warn("No users exist yet and ADMIN_BOOTSTRAP_USERNAME/ADMIN_BOOTSTRAP_PASSWORD are not set — "
                    + "skipping root Admin creation. Set them in application-local.yml to bootstrap the first Admin.");
            return;
        }

        User admin = new User();
        admin.setUsername(bootstrapUsername);
        admin.setPasswordHash(passwordEncoder.encode(bootstrapPassword));
        admin.setRole(UserRole.ADMIN);
        admin.setMustChangePassword(true);
        userRepository.save(admin);

        log.info("Bootstrapped root Admin account '{}'. It must change its password on first login.", bootstrapUsername);
    }
}
