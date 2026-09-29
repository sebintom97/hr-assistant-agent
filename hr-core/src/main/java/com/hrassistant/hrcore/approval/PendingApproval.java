package com.hrassistant.hrcore.approval;

import com.hrassistant.hrcore.leave.LeaveRequestView;

/** One item in a manager's inbox. */
public record PendingApproval(String employeeName, long waitingHours, LeaveRequestView request) {
}
