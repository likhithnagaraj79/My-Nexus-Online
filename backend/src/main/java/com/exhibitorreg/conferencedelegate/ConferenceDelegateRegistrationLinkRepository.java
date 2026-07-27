package com.exhibitorreg.conferencedelegate;

import java.util.UUID;
import org.springframework.data.jpa.repository.JpaRepository;

public interface ConferenceDelegateRegistrationLinkRepository
        extends JpaRepository<ConferenceDelegateRegistrationLink, UUID> {
}
