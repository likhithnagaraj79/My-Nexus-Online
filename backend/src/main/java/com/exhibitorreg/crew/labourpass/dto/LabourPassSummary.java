package com.exhibitorreg.crew.labourpass.dto;

import com.exhibitorreg.crew.labourpass.LabourPass;
import com.exhibitorreg.crew.labourpass.LabourPassType;
import java.time.Instant;
import java.util.UUID;

public record LabourPassSummary(
        UUID id,
        LabourPassType passType,
        int passCount,
        String phoneNumber,
        String stallNumber,
        String issuedByUsername,
        Instant createdAt) {

    public static LabourPassSummary from(LabourPass labourPass) {
        return new LabourPassSummary(
                labourPass.getId(),
                labourPass.getPassType(),
                labourPass.getPassCount(),
                labourPass.getPhoneNumber(),
                labourPass.getStallNumber(),
                labourPass.getIssuedBy().getUsername(),
                labourPass.getCreatedAt());
    }
}
