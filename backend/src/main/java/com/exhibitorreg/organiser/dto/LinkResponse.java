package com.exhibitorreg.organiser.dto;

import java.time.Instant;
import java.util.UUID;

public record LinkResponse(UUID id, String publicUrl, Instant expiresAt, boolean active, Instant createdAt) {
}
