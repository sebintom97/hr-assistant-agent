package com.hrassistant.hrcore.leave;

import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Size;
import java.time.LocalDate;

/**
 * Body of POST /api/leave-requests. @NotNull/@Size are checked by @Valid before our code runs
 * (-> 400). Rules that need data (balance, overlap, "not in the past") are in LeaveService (-> 409/422).
 * There is no employeeId field: you can only ever submit for yourself.
 */
public record SubmitLeaveRequest(
        @NotNull LocalDate startDate,
        @NotNull LocalDate endDate,
        LeaveType leaveType,
        @Size(max = 500) String reason) {

    LeaveType leaveTypeOrDefault() {
        return leaveType == null ? LeaveType.ANNUAL : leaveType;
    }
}
