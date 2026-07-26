package com.exhibitorreg.publicregistration;

import java.util.List;
import java.util.UUID;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.JpaSpecificationExecutor;

public interface ExhibitorPersonRepository
        extends JpaRepository<ExhibitorPerson, UUID>, JpaSpecificationExecutor<ExhibitorPerson> {

    List<ExhibitorPerson> findByCompanyId(UUID companyId);

    long countByPrintedTrue();

    long countByIssuedTrue();
}
