package com.exhibitorreg.organiser.dto;

import java.time.Instant;

public record CreateLinkRequest(Instant expiresAt) {
}
