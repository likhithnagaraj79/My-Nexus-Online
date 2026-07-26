package com.exhibitorreg.organiser.dto;

public record AnalyticsResponse(
        long checkInCount,
        long vendorPassCount,
        long exhibitorPassCount,
        long fabricatorLabourPassCount,
        long printedExhibitorBadgeCount) {
}
