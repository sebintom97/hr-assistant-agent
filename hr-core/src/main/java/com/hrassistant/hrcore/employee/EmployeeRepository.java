package com.hrassistant.hrcore.employee;

import java.util.Collection;
import java.util.List;
import java.util.Optional;
import java.util.UUID;
import org.springframework.data.jpa.repository.JpaRepository;

/**
 * Every finder takes tenantId (rule 5). Spring Data turns the method names into SQL, e.g.
 * findByTenantIdAndManagerIdAndActiveTrue -> WHERE tenant_id = ? AND manager_id = ? AND active = true.
 * Package private: other modules go through EmployeeService, never this repository.
 */
interface EmployeeRepository extends JpaRepository<Employee, UUID> {

    Optional<Employee> findByIdAndActiveTrue(UUID id);

    Optional<Employee> findByTenantIdAndId(UUID tenantId, UUID id);

    List<Employee> findByTenantIdAndIdIn(UUID tenantId, Collection<UUID> ids);

    List<Employee> findByTenantIdAndManagerIdAndActiveTrue(UUID tenantId, UUID managerId);

    List<Employee> findByTenantIdAndTeamIdAndActiveTrue(UUID tenantId, UUID teamId);
}
