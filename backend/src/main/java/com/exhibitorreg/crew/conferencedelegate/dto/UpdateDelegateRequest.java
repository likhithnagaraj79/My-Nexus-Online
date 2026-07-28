package com.exhibitorreg.crew.conferencedelegate.dto;

import jakarta.validation.constraints.Email;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Size;

/** Full-resource replace, same validation as the public form's SubmitDelegateRegistrationRequest
 * — Edit is the "complete the record properly" step, so once saved a delegate is fully valid
 * regardless of how incomplete CSV import left it (e.g. a blank name). */
public record UpdateDelegateRequest(
        @NotBlank @Size(max = 150) String name,
        @NotBlank @Size(max = 200) String companyName,
        @NotBlank @Size(max = 150) String designation,
        @NotBlank @Size(max = 15) String mobileNumber,
        @NotBlank @Email @Size(max = 255) String email) {
}
