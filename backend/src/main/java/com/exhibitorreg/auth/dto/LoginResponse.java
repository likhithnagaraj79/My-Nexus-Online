package com.exhibitorreg.auth.dto;

/**
 * Either {@code totpRequired=true} with a {@code loginTicketId} (Crew/Validator, no tokens yet),
 * or {@code totpRequired=false} with a populated {@code tokens} (Admin/Organiser, or the second
 * TOTP step for Crew/Validator).
 */
public record LoginResponse(boolean totpRequired, String loginTicketId, TokenPair tokens) {

    public static LoginResponse totpRequired(String loginTicketId) {
        return new LoginResponse(true, loginTicketId, null);
    }

    public static LoginResponse authenticated(TokenPair tokens) {
        return new LoginResponse(false, null, tokens);
    }
}
