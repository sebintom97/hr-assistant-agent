package com.hrassistant.hrcore.leave;

/** Matches the CHECK constraint leave_request_type_valid. Only ANNUAL counts against the balance. */
public enum LeaveType {
    ANNUAL,
    SICK,
    UNPAID
}
