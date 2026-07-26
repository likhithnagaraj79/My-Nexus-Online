package com.exhibitorreg.auth;

import java.util.UUID;

/**
 * The Spring Security principal resolved entirely from JWT claims — no DB hit needed
 * to answer "who is making this request" for every authenticated endpoint.
 */
public record AuthenticatedPrincipal(UUID userId, String username, UserRole role, boolean mustChangePassword) {
}
