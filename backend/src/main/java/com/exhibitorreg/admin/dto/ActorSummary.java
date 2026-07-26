package com.exhibitorreg.admin.dto;

import com.exhibitorreg.auth.User;
import com.exhibitorreg.auth.UserRole;
import java.time.Instant;
import java.util.UUID;

public record ActorSummary(
        UUID id,
        String username,
        String email,
        UserRole role,
        boolean active,
        boolean accountLocked,
        Instant createdAt) {

    public static ActorSummary from(User user) {
        return new ActorSummary(
                user.getId(),
                user.getUsername(),
                user.getEmail(),
                user.getRole(),
                user.isActive(),
                user.isAccountLocked(),
                user.getCreatedAt());
    }
}
