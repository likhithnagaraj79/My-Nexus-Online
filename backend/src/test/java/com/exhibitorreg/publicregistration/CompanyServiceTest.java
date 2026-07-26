package com.exhibitorreg.publicregistration;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import java.time.Duration;
import java.util.Optional;
import java.util.UUID;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.data.redis.core.RedisTemplate;
import org.springframework.data.redis.core.ValueOperations;
import org.springframework.test.util.ReflectionTestUtils;

@ExtendWith(MockitoExtension.class)
class CompanyServiceTest {

    @Mock
    private CompanyRepository companyRepository;

    @Mock
    private RedisTemplate<String, String> redisTemplate;

    @Mock
    private ValueOperations<String, String> valueOperations;

    private CompanyService service;

    @BeforeEach
    void setUp() {
        service = new CompanyService(companyRepository, redisTemplate);
    }

    private static Company companyWithId(String name) {
        Company company = new Company();
        ReflectionTestUtils.setField(company, "id", UUID.randomUUID());
        company.setName(name);
        return company;
    }

    @Test
    void returnsExistingCompanyWithoutTouchingRedisWhenFound() {
        Company existing = companyWithId("Acme Exhibits");
        when(companyRepository.findByNameIgnoreCase("Acme Exhibits")).thenReturn(Optional.of(existing));

        Company result = service.findOrCreate("Acme Exhibits");

        assertThat(result).isEqualTo(existing);
        verify(redisTemplate, never()).opsForValue();
    }

    @Test
    void createsNewCompanyWithLockWhenNotFound() {
        when(companyRepository.findByNameIgnoreCase("New Co")).thenReturn(Optional.empty());
        when(redisTemplate.opsForValue()).thenReturn(valueOperations);
        when(valueOperations.setIfAbsent("companylock:new co", "1", Duration.ofMillis(5000))).thenReturn(true);

        Company result = service.findOrCreate("New Co");

        assertThat(result.getName()).isEqualTo("New Co");
        verify(companyRepository).save(any(Company.class));
        verify(redisTemplate).delete("companylock:new co");
    }

    @Test
    void trimsWhitespaceBeforeLookup() {
        Company existing = companyWithId("Acme Exhibits");
        when(companyRepository.findByNameIgnoreCase("Acme Exhibits")).thenReturn(Optional.of(existing));

        Company result = service.findOrCreate("  Acme Exhibits  ");

        assertThat(result).isEqualTo(existing);
    }
}
