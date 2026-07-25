package com.exhibitorreg.validator;

import java.util.UUID;
import org.springframework.data.jpa.repository.JpaRepository;

public interface CheckInScanRepository extends JpaRepository<CheckInScan, UUID> {
}
