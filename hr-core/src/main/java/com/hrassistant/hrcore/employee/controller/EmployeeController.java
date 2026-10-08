package com.hrassistant.hrcore.employee.controller;

import java.util.UUID;

import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import com.hrassistant.hrcore.common.security.CurrentUser;
import com.hrassistant.hrcore.employee.dto.EmployeeResponse;
import com.hrassistant.hrcore.employee.service.EmployeeService;
import org.springframework.web.bind.annotation.RequestParam;


@RestController
@RequestMapping("/api/v1/employees")
public class EmployeeController {

    private final EmployeeService employeeService;

    EmployeeController(EmployeeService employeeService){
        this.employeeService = employeeService;
    }

    @GetMapping("/me")
    public EmployeeResponse getUser(CurrentUser me) {
        return employeeService.getEmployee(me.tenantId(),me.employeeId());
    }
    

    @GetMapping("/{id}")
    public EmployeeResponse getEmployee(CurrentUser me,@PathVariable UUID id){
        return employeeService.getEmployee(me.tenantId(), id);
    }
 
}