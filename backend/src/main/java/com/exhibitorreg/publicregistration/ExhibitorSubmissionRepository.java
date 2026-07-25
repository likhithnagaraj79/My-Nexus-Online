package com.exhibitorreg.publicregistration;

import java.util.UUID;
import org.springframework.data.jpa.repository.JpaRepository;

public interface ExhibitorSubmissionRepository extends JpaRepository<ExhibitorSubmission, UUID> {
}
