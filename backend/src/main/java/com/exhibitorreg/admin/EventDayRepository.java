package com.exhibitorreg.admin;

import java.util.List;
import java.util.UUID;
import org.springframework.data.jpa.repository.JpaRepository;

public interface EventDayRepository extends JpaRepository<EventDay, UUID> {

    List<EventDay> findByEventIdOrderByDayNumber(UUID eventId);
}
