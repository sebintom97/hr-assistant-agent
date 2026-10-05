package com.hrassistant.hrcore.employee.repository;

import java.util.Optional;
import java.util.UUID;
import com.hrassistant.hrcore.employee.entity.Employee;

import org.springframework.data.jpa.repository.JpaRepository;

public interface EmployeeRepository extends JpaRepository<Employee, UUID> {

    Optional<Employee> findByTenantIdAndIdAndActiveTrue(UUID tenantId, UUID id);
    // Identity lookup ONLY (used to find out who is calling). No tenant filter on purpose.
   // Never use it to look up other employees.
    Optional<Employee> findByIdAndActiveTrue(UUID employeeId);
    
}
