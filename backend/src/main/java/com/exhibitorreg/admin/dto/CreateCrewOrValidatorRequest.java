package com.exhibitorreg.admin.dto;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Pattern;
import jakarta.validation.constraints.Size;

public record CreateCrewOrValidatorRequest(
        @NotBlank @Size(max = 100) String username,
        @NotBlank @Size(min = 8, max = 100) String temporaryPassword,
        @NotBlank @Pattern(regexp = "\\d{12}", message = "must be a 12-digit Aadhar number") String aadharNumber,
        @NotBlank @Size(max = 15) String phoneNumber) {
}
