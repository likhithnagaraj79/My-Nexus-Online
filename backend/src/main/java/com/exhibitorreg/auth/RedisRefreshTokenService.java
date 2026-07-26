package com.exhibitorreg.auth;

import java.time.Duration;
import java.util.Optional;
import java.util.UUID;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.data.redis.core.RedisTemplate;
import org.springframework.stereotype.Service;

/**
 * Refresh tokens are keyed per opaque token (not per user), so a user can hold multiple
 * concurrent sessions (e.g. desktop + tablet); logout/rotation only revokes that one session.
 */
@Service
public class RedisRefreshTokenService implements RefreshTokenService {

    private static final String KEY_PREFIX = "refresh:";

    private final RedisTemplate<String, String> redisTemplate;
    private final Duration ttl;

    public RedisRefreshTokenService(
            RedisTemplate<String, String> redisTemplate,
            @Value("${app.jwt.refresh-token-ttl-days}") long refreshTokenTtlDays) {
        this.redisTemplate = redisTemplate;
        this.ttl = Duration.ofDays(refreshTokenTtlDays);
    }

    @Override
    public String issue(UUID userId) {
        String token = UUID.randomUUID().toString();
        redisTemplate.opsForValue().set(KEY_PREFIX + token, userId.toString(), ttl);
        return token;
    }

    @Override
    public Optional<Rotated> rotate(String oldToken) {
        String userIdValue = redisTemplate.opsForValue().get(KEY_PREFIX + oldToken);
        if (userIdValue == null) {
            return Optional.empty();
        }
        redisTemplate.delete(KEY_PREFIX + oldToken);
        UUID userId = UUID.fromString(userIdValue);
        return Optional.of(new Rotated(issue(userId), userId));
    }

    @Override
    public void revoke(String token) {
        redisTemplate.delete(KEY_PREFIX + token);
    }
}
