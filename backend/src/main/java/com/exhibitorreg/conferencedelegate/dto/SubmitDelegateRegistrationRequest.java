package com.exhibitorreg.conferencedelegate.dto;

import jakarta.validation.constraints.Email;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Size;

public record SubmitDelegateRegistrationRequest(
        @NotBlank @Size(max = 150) String name,
        @NotBlank @Size(max = 200) String companyName,
        @NotBlank @Size(max = 150) String designation,
        @NotBlank @Size(max = 15) String mobileNumber,
        @NotBlank @Email @Size(max = 255) String email,
        @NotBlank String recaptchaToken) {
}
