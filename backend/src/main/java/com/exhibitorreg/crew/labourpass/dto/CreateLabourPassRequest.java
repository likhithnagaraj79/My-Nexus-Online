package com.exhibitorreg.crew.labourpass.dto;

import com.exhibitorreg.crew.labourpass.LabourPassType;
import jakarta.validation.constraints.AssertTrue;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Positive;

public record CreateLabourPassRequest(
        @NotNull LabourPassType passType,
        @Positive int passCount,
        @NotBlank String phoneNumber,
        String stallNumber) {

    @AssertTrue(message = "stallNumber is required for EXHIBITOR and FABRICATOR_LABOUR pass types")
    public boolean isStallNumberValid() {
        return passType == LabourPassType.VENDOR || (stallNumber != null && !stallNumber.isBlank());
    }
}
