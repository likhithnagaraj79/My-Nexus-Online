package com.exhibitorreg.crew.conferencedelegate.dto;

import com.exhibitorreg.conferencedelegate.ConferenceDelegate;
import java.time.Instant;
import java.util.UUID;

public record DelegatePassSummary(
        UUID id,
        String name,
        String designation,
        String companyName,
        String mobileNumber,
        String email,
        boolean printed,
        Instant printedAt) {

    public static DelegatePassSummary from(ConferenceDelegate delegate) {
        return new DelegatePassSummary(
                delegate.getId(),
                delegate.getName(),
                delegate.getDesignation(),
                delegate.getCompanyName(),
                delegate.getMobileNumber(),
                delegate.getEmail(),
                delegate.isPrinted(),
                delegate.getPrintedAt());
    }
}
