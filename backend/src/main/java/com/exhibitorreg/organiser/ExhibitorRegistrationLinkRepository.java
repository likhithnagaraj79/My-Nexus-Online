package com.exhibitorreg.organiser;

import java.util.UUID;
import org.springframework.data.jpa.repository.JpaRepository;

public interface ExhibitorRegistrationLinkRepository extends JpaRepository<ExhibitorRegistrationLink, UUID> {
}
