package com.exhibitorreg.crew.exhibitorpass.dto;

import jakarta.validation.constraints.NotBlank;

public record IssueRequest(@NotBlank String phoneNumber) {
}
