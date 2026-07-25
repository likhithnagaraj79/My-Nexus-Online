package com.exhibitorreg.auth;

import static org.assertj.core.api.Assertions.assertThat;

import com.exhibitorreg.config.JpaAuditingConfig;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.data.jpa.test.autoconfigure.DataJpaTest;
import org.springframework.boot.jdbc.test.autoconfigure.AutoConfigureTestDatabase;
import org.springframework.context.annotation.Import;

@DataJpaTest
@AutoConfigureTestDatabase(replace = AutoConfigureTestDatabase.Replace.NONE)
@Import(JpaAuditingConfig.class)
class UserRepositoryTest {

    @Autowired
    private UserRepository userRepository;

    @Test
    void savesAndFetchesUserByUsername() {
        User user = new User();
        user.setUsername("admin");
        user.setPasswordHash("hashed-password");
        user.setRole(UserRole.ADMIN);

        userRepository.saveAndFlush(user);

        User found = userRepository.findByUsername("admin").orElseThrow();

        assertThat(found.getId()).isNotNull();
        assertThat(found.getFailedLoginAttempts()).isZero();
        assertThat(found.isAccountLocked()).isFalse();
        assertThat(found.isMustChangePassword()).isTrue();
        assertThat(found.getCreatedAt()).isNotNull();
        assertThat(found.getUpdatedAt()).isNotNull();
    }
}
