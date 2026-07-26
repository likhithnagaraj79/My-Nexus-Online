package com.exhibitorreg.auth.dto;

public record TokenPair(String accessToken, String refreshToken, boolean mustChangePassword) {
}
