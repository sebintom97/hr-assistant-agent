package com.hrassistant.hrcore.employee;

import java.util.Optional;
import java.util.UUID;
import org.springframework.data.jpa.repository.JpaRepository;

interface TeamRepository extends JpaRepository<Team, UUID> {

    Optional<Team> findByTenantIdAndId(UUID tenantId, UUID id);
}
