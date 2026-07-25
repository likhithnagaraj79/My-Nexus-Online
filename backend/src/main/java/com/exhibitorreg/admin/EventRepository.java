package com.exhibitorreg.admin;

import java.util.Optional;
import java.util.UUID;
import org.springframework.data.jpa.repository.JpaRepository;

public interface EventRepository extends JpaRepository<Event, UUID> {

    Optional<Event> findByActiveTrue();
}
