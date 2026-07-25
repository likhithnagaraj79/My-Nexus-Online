package com.exhibitorreg.publicregistration;

import java.util.List;
import java.util.UUID;
import org.springframework.data.jpa.repository.JpaRepository;

public interface ExhibitorPersonRepository extends JpaRepository<ExhibitorPerson, UUID> {

    List<ExhibitorPerson> findByCompanyId(UUID companyId);
}
