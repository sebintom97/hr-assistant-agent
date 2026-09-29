package com.hrassistant.hrcore.employee;

import com.hrassistant.hrcore.common.ApiException;
import java.util.Collection;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.UUID;
import java.util.function.Function;
import java.util.stream.Collectors;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

/**
 * The employee module's front door. Other modules (leave, approval) call this,
 * never EmployeeRepository or the employee table directly.
 */
@Service
@Transactional(readOnly = true)
public class EmployeeService {

    private final EmployeeRepository employees;
    private final TeamRepository teams;

    EmployeeService(EmployeeRepository employees, TeamRepository teams) {
        this.employees = employees;
        this.teams = teams;
    }

    /**
     * The only lookup without a tenant filter: it's how we find out the caller's tenant in the first
     * place (see CurrentUserResolver). Everything after this uses the tenant from the result.
     */
    public Optional<Employee> findActive(UUID employeeId) {
        return employees.findByIdAndActiveTrue(employeeId);
    }

    public Employee get(UUID tenantId, UUID employeeId) {
        return employees.findByTenantIdAndId(tenantId, employeeId)
                .orElseThrow(() -> new ApiException.NotFound("Employee"));
    }

    public Map<UUID, Employee> getAll(UUID tenantId, Collection<UUID> employeeIds) {
        if (employeeIds.isEmpty()) {
            return Map.of();
        }
        return employees.findByTenantIdAndIdIn(tenantId, employeeIds).stream()
                .collect(Collectors.toMap(Employee::getId, Function.identity()));
    }

    public List<Employee> directReports(UUID tenantId, UUID managerId) {
        return employees.findByTenantIdAndManagerIdAndActiveTrue(tenantId, managerId);
    }

    public List<Employee> teamMembers(UUID tenantId, UUID teamId) {
        return teamId == null ? List.of() : employees.findByTenantIdAndTeamIdAndActiveTrue(tenantId, teamId);
    }

    public EmployeeProfile profile(UUID tenantId, UUID employeeId) {
        Employee employee = get(tenantId, employeeId);
        String teamName = employee.getTeamId() == null ? null
                : teams.findByTenantIdAndId(tenantId, employee.getTeamId()).map(Team::getName).orElse(null);
        Employee manager = employee.getManagerId() == null ? null : get(tenantId, employee.getManagerId());
        return EmployeeProfile.of(employee, teamName, manager);
    }
}
