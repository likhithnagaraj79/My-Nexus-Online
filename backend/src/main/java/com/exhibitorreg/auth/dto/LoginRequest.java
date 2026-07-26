package com.exhibitorreg.auth.dto;

import com.exhibitorreg.auth.UserRole;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;

public record LoginRequest(
        @NotNull UserRole role,
        @NotBlank String username,
        @NotBlank String password) {
}
