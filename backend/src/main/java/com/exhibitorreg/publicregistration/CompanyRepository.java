package com.exhibitorreg.publicregistration;

import java.util.List;
import java.util.Optional;
import java.util.UUID;
import org.springframework.data.jpa.repository.JpaRepository;

public interface CompanyRepository extends JpaRepository<Company, UUID> {

    List<Company> findByNameContainingIgnoreCase(String namePart);

    Optional<Company> findByNameIgnoreCase(String name);
}
