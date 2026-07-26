package com.exhibitorreg.crew.exhibitorpass.dto;

import com.exhibitorreg.publicregistration.ExhibitorPerson;
import java.time.Instant;
import java.util.UUID;

public record ExhibitorPassSummary(
        UUID id,
        String name,
        String designation,
        UUID companyId,
        String companyName,
        boolean printed,
        Instant printedAt,
        boolean issued,
        Instant issuedAt) {

    public static ExhibitorPassSummary from(ExhibitorPerson person) {
        return new ExhibitorPassSummary(
                person.getId(),
                person.getName(),
                person.getDesignation(),
                person.getCompany().getId(),
                person.getCompany().getName(),
                person.isPrinted(),
                person.getPrintedAt(),
                person.isIssued(),
                person.getIssuedAt());
    }
}
