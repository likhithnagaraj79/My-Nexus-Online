package com.exhibitorreg.organiser.dto;

public record DashboardResponse(
        long printedBadgeCount, long issuedBadgeCount, long submissionCount, long exhibitorPersonCount, long checkInCount) {
}
