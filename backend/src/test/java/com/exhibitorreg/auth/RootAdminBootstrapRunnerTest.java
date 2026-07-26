package com.exhibitorreg.auth;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.ArgumentCaptor;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.security.crypto.password.PasswordEncoder;

@ExtendWith(MockitoExtension.class)
class RootAdminBootstrapRunnerTest {

    @Mock
    private UserRepository userRepository;

    @Mock
    private PasswordEncoder passwordEncoder;

    @Test
    void doesNothingWhenUsersAlreadyExist() throws Exception {
        when(userRepository.count()).thenReturn(1L);
        var runner = new RootAdminBootstrapRunner(userRepository, passwordEncoder, "admin", "changeme");

        runner.run();

        verify(userRepository, never()).save(any());
    }

    @Test
    void doesNothingWhenBootstrapCredentialsAreMissing() throws Exception {
        when(userRepository.count()).thenReturn(0L);
        var runner = new RootAdminBootstrapRunner(userRepository, passwordEncoder, "", "");

        runner.run();

        verify(userRepository, never()).save(any());
    }

    @Test
    void createsRootAdminWhenEmptyAndCredentialsProvided() throws Exception {
        when(userRepository.count()).thenReturn(0L);
        when(passwordEncoder.encode("changeme")).thenReturn("hashed");
        var runner = new RootAdminBootstrapRunner(userRepository, passwordEncoder, "admin", "changeme");

        runner.run();

        ArgumentCaptor<User> captor = ArgumentCaptor.forClass(User.class);
        verify(userRepository).save(captor.capture());
        User created = captor.getValue();
        assertThat(created.getUsername()).isEqualTo("admin");
        assertThat(created.getPasswordHash()).isEqualTo("hashed");
        assertThat(created.getRole()).isEqualTo(UserRole.ADMIN);
        assertThat(created.isMustChangePassword()).isTrue();
    }
}
