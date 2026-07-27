package com.exhibitorreg.crew.conferencedelegate.dto;

import com.exhibitorreg.conferencedelegate.ConferenceDelegate;
import java.time.Instant;
import java.util.UUID;

public record DelegatePassSummary(
        UUID id, String name, String designation, String companyName, boolean printed, Instant printedAt) {

    public static DelegatePassSummary from(ConferenceDelegate delegate) {
        return new DelegatePassSummary(
                delegate.getId(),
                delegate.getName(),
                delegate.getDesignation(),
                delegate.getCompanyName(),
                delegate.isPrinted(),
                delegate.getPrintedAt());
    }
}
