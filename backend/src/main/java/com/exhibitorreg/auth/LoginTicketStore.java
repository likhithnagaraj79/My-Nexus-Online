package com.exhibitorreg.auth;

import java.time.Duration;
import java.util.Optional;
import java.util.UUID;
import org.springframework.data.redis.core.RedisTemplate;
import org.springframework.stereotype.Service;

/**
 * Short-lived proof that a Crew/Validator user has already passed the password step and
 * now only needs to submit a TOTP code. Kept separate from {@link RefreshTokenService}
 * since it's a different purpose and a much shorter TTL — conflating the two would blur
 * "proof password step passed" with "long-lived session token."
 */
@Service
public class LoginTicketStore {

    private static final String KEY_PREFIX = "login-ticket:";
    private static final Duration TTL = Duration.ofMinutes(3);

    private final RedisTemplate<String, String> redisTemplate;

    public LoginTicketStore(RedisTemplate<String, String> redisTemplate) {
        this.redisTemplate = redisTemplate;
    }

    public String issue(UUID userId) {
        String ticketId = UUID.randomUUID().toString();
        redisTemplate.opsForValue().set(KEY_PREFIX + ticketId, userId.toString(), TTL);
        return ticketId;
    }

    public Optional<UUID> consume(String ticketId) {
        String userIdValue = redisTemplate.opsForValue().get(KEY_PREFIX + ticketId);
        if (userIdValue == null) {
            return Optional.empty();
        }
        redisTemplate.delete(KEY_PREFIX + ticketId);
        return Optional.of(UUID.fromString(userIdValue));
    }
}
