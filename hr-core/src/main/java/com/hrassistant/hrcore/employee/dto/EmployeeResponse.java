package com.hrassistant.hrcore.employee.dto;

import java.util.UUID;

import com.hrassistant.hrcore.employee.entity.Role;

public record EmployeeResponse(UUID id,
    UUID teamId,
    UUID managerId,
    String email,
    String name,
    String jobTitle,
    Role role) {
    
}
