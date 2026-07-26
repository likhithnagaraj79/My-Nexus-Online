package com.exhibitorreg.publicregistration.dto;

import jakarta.validation.Valid;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Size;
import java.util.List;

public record SubmitRegistrationRequest(
        @NotBlank @Size(max = 200) String companyName,
        @NotNull @Size(min = 1, max = 9) List<@Valid PersonInput> people,
        @NotBlank String recaptchaToken) {

    public record PersonInput(
            @NotBlank @Size(max = 150) String name, @NotBlank @Size(max = 150) String designation) {
    }
}
