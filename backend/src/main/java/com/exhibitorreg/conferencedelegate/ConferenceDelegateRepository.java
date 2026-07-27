package com.exhibitorreg.conferencedelegate;

import java.util.UUID;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.JpaSpecificationExecutor;

public interface ConferenceDelegateRepository
        extends JpaRepository<ConferenceDelegate, UUID>, JpaSpecificationExecutor<ConferenceDelegate> {
}
