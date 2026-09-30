package com.hrassistant.hrcore.employee;

import java.time.LocalDate;
import java.util.UUID;

/** Response for GET /api/me. A DTO: the API never returns JPA entities directly. */
public record EmployeeProfile(
        UUID id,
        String name,
        String email,
        String jobTitle,
        Role role,
        LocalDate hireDate,
        UUID teamId,
        String teamName,
        UUID managerId,
        String managerName) {

    static EmployeeProfile of(Employee e, String teamName, Employee manager) {
        return new EmployeeProfile(e.getId(), e.fullName(), e.getEmail(), e.getJobTitle(), e.getRole(),
                e.getHireDate(), e.getTeamId(), teamName,
                manager == null ? null : manager.getId(), manager == null ? null : manager.fullName());
    }
}
