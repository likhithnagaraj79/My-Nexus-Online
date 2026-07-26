package com.exhibitorreg.admin.dto;

import java.util.UUID;

/** {@code totpQrPngBase64} is null for Organisers (no TOTP), populated for Crew/Validator. */
public record CreatedActorResponse(UUID userId, String totpQrPngBase64) {
}
