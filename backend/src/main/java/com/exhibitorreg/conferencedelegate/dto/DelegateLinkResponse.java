package com.exhibitorreg.conferencedelegate.dto;

import java.time.Instant;
import java.util.UUID;

public record DelegateLinkResponse(UUID id, String publicUrl, Instant expiresAt, boolean active, Instant createdAt) {
}
