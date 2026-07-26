package com.exhibitorreg.auth.dto;

import jakarta.validation.constraints.NotBlank;

public record TotpLoginRequest(@NotBlank String loginTicketId, @NotBlank String code) {
}
