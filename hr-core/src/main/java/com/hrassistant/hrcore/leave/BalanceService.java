package com.hrassistant.hrcore.leave;

import java.math.BigDecimal;
import java.math.RoundingMode;
import java.time.LocalDate;
import java.util.UUID;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Service
@Transactional(readOnly = true)
public class BalanceService {

    private final LeaveAllowanceRepository allowances;
    private final LeaveRequestRepository requests;

    BalanceService(LeaveAllowanceRepository allowances, LeaveRequestRepository requests) {
        this.allowances = allowances;
        this.requests = requests;
    }

    /** A request counts against the year it STARTS in (simplification, see ADR 004). */
    public LeaveBalance balance(UUID tenantId, UUID employeeId, int year) {
        BigDecimal allowance = allowances.findByTenantIdAndEmployeeIdAndYear(tenantId, employeeId, year)
                .map(LeaveAllowance::getDays)
                .orElse(BigDecimal.ZERO);
        LocalDate from = LocalDate.of(year, 1, 1);
        LocalDate to = LocalDate.of(year, 12, 31);
        BigDecimal approved = requests.sumWorkingDays(tenantId, employeeId, LeaveStatus.APPROVED, LeaveType.ANNUAL, from, to);
        BigDecimal pending = requests.sumWorkingDays(tenantId, employeeId, LeaveStatus.PENDING, LeaveType.ANNUAL, from, to);
        BigDecimal remaining = allowance.subtract(approved);
        return new LeaveBalance(year, oneDecimal(allowance), oneDecimal(approved), oneDecimal(pending),
                oneDecimal(remaining), oneDecimal(remaining.subtract(pending)));
    }

    /** Always "2.0", never "2" or "2.00", so the API output is consistent. */
    private static BigDecimal oneDecimal(BigDecimal value) {
        return value.setScale(1, RoundingMode.UNNECESSARY);
    }
}
