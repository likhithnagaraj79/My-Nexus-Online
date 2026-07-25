package com.exhibitorreg.crew.labourpass;

import java.util.UUID;
import org.springframework.data.jpa.repository.JpaRepository;

public interface LabourPassRepository extends JpaRepository<LabourPass, UUID> {
}
