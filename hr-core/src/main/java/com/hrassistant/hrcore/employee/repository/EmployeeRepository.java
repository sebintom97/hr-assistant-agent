package com.hrassistant.hrcore.employee.repository;

import java.util.Optional;
import java.util.UUID;
import com.hrassistant.hrcore.employee.entity.Employee;

import org.springframework.data.jpa.repository.JpaRepository;

public interface EmployeeRepository extends JpaRepository<Employee, UUID> {

    Optional<Employee>findByTenantIdAndId(UUID tenantId, UUID id);
    
}
