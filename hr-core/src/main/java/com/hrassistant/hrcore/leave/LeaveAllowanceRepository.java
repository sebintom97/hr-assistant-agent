package com.hrassistant.hrcore.leave;

import java.util.Optional;
import java.util.UUID;
import org.springframework.data.jpa.repository.JpaRepository;

interface LeaveAllowanceRepository extends JpaRepository<LeaveAllowance, UUID> {

    Optional<LeaveAllowance> findByTenantIdAndEmployeeIdAndYear(UUID tenantId, UUID employeeId, int year);
}
