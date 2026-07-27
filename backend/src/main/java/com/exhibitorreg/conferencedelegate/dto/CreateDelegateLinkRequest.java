package com.exhibitorreg.conferencedelegate.dto;

import java.time.Instant;

public record CreateDelegateLinkRequest(Instant expiresAt) {
}
