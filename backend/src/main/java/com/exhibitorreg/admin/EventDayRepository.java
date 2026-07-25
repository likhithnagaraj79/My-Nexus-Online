package com.exhibitorreg.admin;

import java.util.UUID;
import org.springframework.data.jpa.repository.JpaRepository;

public interface EventDayRepository extends JpaRepository<EventDay, UUID> {
}
