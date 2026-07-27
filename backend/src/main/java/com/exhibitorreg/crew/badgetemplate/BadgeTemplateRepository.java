package com.exhibitorreg.crew.badgetemplate;

import java.util.Optional;
import java.util.UUID;
import org.springframework.data.jpa.repository.JpaRepository;

public interface BadgeTemplateRepository extends JpaRepository<BadgeTemplate, UUID> {

    Optional<BadgeTemplate> findFirstByOrderByCreatedAtAsc();
}
