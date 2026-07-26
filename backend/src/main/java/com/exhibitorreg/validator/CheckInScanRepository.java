package com.exhibitorreg.validator;

import java.util.List;
import java.util.UUID;
import org.springframework.data.jpa.repository.JpaRepository;

public interface CheckInScanRepository extends JpaRepository<CheckInScan, UUID> {

    List<CheckInScan> findByEventDayId(UUID eventDayId);

    boolean existsByExhibitorPersonIdAndEventDayId(UUID exhibitorPersonId, UUID eventDayId);
}
