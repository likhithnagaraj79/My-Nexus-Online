package com.exhibitorreg.conferencedelegate.dto;

import java.util.List;

public record DelegateImportSummary(int importedCount, List<DelegateImportRowError> errors) {
}
