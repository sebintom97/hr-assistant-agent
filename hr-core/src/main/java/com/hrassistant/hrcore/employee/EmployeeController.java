package com.hrassistant.hrcore.employee;

import com.hrassistant.hrcore.common.CurrentUser;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequestMapping("/api/me")
@Tag(name = "Me")
class EmployeeController {

    private final EmployeeService employees;

    EmployeeController(EmployeeService employees) {
        this.employees = employees;
    }

    @GetMapping
    @Operation(summary = "Who am I: name, role, team and manager")
    EmployeeProfile me(CurrentUser me) {
        return employees.profile(me.tenantId(), me.employeeId());
    }
}
