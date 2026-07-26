package com.exhibitorreg.auth;

import java.util.Optional;
import java.util.UUID;

public interface RefreshTokenService {

    String issue(UUID userId);

    Optional<Rotated> rotate(String oldToken);

    void revoke(String token);

    record Rotated(String refreshToken, UUID userId) {
    }
}
