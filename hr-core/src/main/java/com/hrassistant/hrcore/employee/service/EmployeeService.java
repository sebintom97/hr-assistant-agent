package com.hrassistant.hrcore.employee.service;

import java.util.UUID;

import org.springframework.http.HttpStatus;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.web.server.ResponseStatusException;

import com.hrassistant.hrcore.employee.dto.EmployeeResponse;
import com.hrassistant.hrcore.employee.repository.EmployeeRepository;
import com.hrassistant.hrcore.employee.entity.Employee;
import com.hrassistant.hrcore.common.security.CurrentUser;


@Service
public class EmployeeService {
    private final EmployeeRepository employeeRepository;

    EmployeeService(EmployeeRepository employeeRepository) {
        this.employeeRepository = employeeRepository;
    }

    @Transactional(readOnly = true)
    public EmployeeResponse getEmployee(UUID tenantId, UUID id) {
        Employee employee = employeeRepository.findByTenantIdAndIdAndActiveTrue(tenantId, id)
        .orElseThrow(() -> new ResponseStatusException(HttpStatus.NOT_FOUND,"Employee not found"));

        return new EmployeeResponse(
            employee.getId(),
            employee.getTeamId(),
            employee.getManagerId(),
            employee.getEmail(),
            employee.getFirstName() + " " + employee.getLastName(),
            employee.getJobTitle(),
            employee.getRole()
        );
    }

    @Transactional(readOnly= true)
    public CurrentUser getCurrentUser(UUID employeeId){
        Employee employee = employeeRepository.findByIdAndActiveTrue(employeeId)
        .orElseThrow(() -> new ResponseStatusException(HttpStatus.UNAUTHORIZED,"Unknown or inactive employee"));

        return new CurrentUser(
            employee.getId(),
            employee.getTenantId()
        );
    }

}
