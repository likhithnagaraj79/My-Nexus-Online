package com.exhibitorreg.publicregistration.dto;

import java.util.UUID;

public record SubmissionResponse(UUID submissionId, UUID companyId, int personCount) {
}
