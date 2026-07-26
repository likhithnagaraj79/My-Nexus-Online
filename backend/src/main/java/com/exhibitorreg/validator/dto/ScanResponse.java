package com.exhibitorreg.validator.dto;

import java.time.Instant;
import java.util.UUID;

public record ScanResponse(
        UUID checkInScanId,
        UUID exhibitorPersonId,
        String personName,
        String companyName,
        boolean alreadyCheckedInToday,
        Instant scannedAt) {
}
