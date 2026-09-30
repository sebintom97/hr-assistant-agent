package com.hrassistant.hrcore.leave;

import java.math.BigDecimal;
import java.time.Instant;
import java.time.LocalDate;
import java.util.UUID;

/** API representation of a leave request. The entity itself never leaves the service layer. */
public record LeaveRequestView(
        UUID id,
        UUID employeeId,
        LeaveType leaveType,
        LocalDate startDate,
        LocalDate endDate,
        BigDecimal workingDays,
        String reason,
        LeaveStatus status,
        UUID decidedById,
        Instant decidedAt,
        String decisionNote,
        Instant cancelledAt,
        Instant createdAt) {

    public static LeaveRequestView of(LeaveRequest r) {
        return new LeaveRequestView(r.getId(), r.getEmployeeId(), r.getLeaveType(), r.getStartDate(),
                r.getEndDate(), r.getWorkingDays(), r.getReason(), r.getStatus(), r.getDecidedById(),
                r.getDecidedAt(), r.getDecisionNote(), r.getCancelledAt(), r.getCreatedAt());
    }
}
