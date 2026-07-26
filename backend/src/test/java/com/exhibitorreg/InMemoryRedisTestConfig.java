package com.exhibitorreg;

import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.Mockito.doAnswer;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.when;

import java.time.Duration;
import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;
import org.springframework.boot.test.context.TestConfiguration;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Primary;
import org.springframework.data.redis.core.RedisTemplate;
import org.springframework.data.redis.core.ValueOperations;

/**
 * Replaces the real Redis-backed template with an in-memory Mockito fake so the end-to-end
 * flows don't require a live Redis instance (keeps the project's no-Docker/no-Testcontainers
 * convention) while still exercising RefreshTokenService/LoginTicketStore/CompanyService for
 * real. Real-Redis behavior is verified manually against local Homebrew Redis instead.
 */
@TestConfiguration
public class InMemoryRedisTestConfig {

    @Bean
    @Primary
    @SuppressWarnings("unchecked")
    public RedisTemplate<String, String> testRedisTemplate() {
        Map<String, String> store = new ConcurrentHashMap<>();

        ValueOperations<String, String> valueOps = mock(ValueOperations.class);
        when(valueOps.get(any())).thenAnswer(inv -> store.get(inv.getArgument(0).toString()));
        doAnswer(inv -> {
            store.put(inv.getArgument(0), inv.getArgument(1));
            return null;
        }).when(valueOps).set(anyString(), anyString(), any(Duration.class));
        when(valueOps.setIfAbsent(anyString(), anyString(), any(Duration.class)))
                .thenAnswer(inv -> store.putIfAbsent(inv.getArgument(0), inv.getArgument(1)) == null);

        RedisTemplate<String, String> template = mock(RedisTemplate.class);
        when(template.opsForValue()).thenReturn(valueOps);
        when(template.delete(anyString())).thenAnswer(inv -> store.remove((String) inv.getArgument(0)) != null);

        return template;
    }
}
