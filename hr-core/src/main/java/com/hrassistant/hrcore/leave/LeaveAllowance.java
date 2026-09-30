package com.hrassistant.hrcore.leave;

import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.Id;
import jakarta.persistence.Table;
import java.math.BigDecimal;
import java.util.UUID;

@Entity
@Table(name = "leave_allowance")
class LeaveAllowance {

    @Id
    private UUID id;

    @Column(name = "tenant_id")
    private UUID tenantId;

    @Column(name = "employee_id")
    private UUID employeeId;

    private int year;

    private BigDecimal days;

    protected LeaveAllowance() {
        // for JPA
    }

    BigDecimal getDays() {
        return days;
    }
}
