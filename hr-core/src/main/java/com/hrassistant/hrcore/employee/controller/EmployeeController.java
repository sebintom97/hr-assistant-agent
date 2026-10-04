package com.hrassistant.hrcore.employee.controller;

import java.util.UUID;

import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.RequestHeader;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import com.hrassistant.hrcore.employee.dto.EmployeeResponse;
import com.hrassistant.hrcore.employee.service.EmployeeService;

@RestController
@RequestMapping ("/api/v1/employees")
public class EmployeeController {

    private final EmployeeService employeeService;

    EmployeeController(EmployeeService employeeService){
        this.employeeService = employeeService;
    }

    @GetMapping("/{id}")
    public EmployeeResponse getEmployee(@RequestHeader ("X-Tenant-Id") UUID tenantId,@PathVariable UUID id){
        return employeeService.getEmployee(tenantId, id);
    }

    
}