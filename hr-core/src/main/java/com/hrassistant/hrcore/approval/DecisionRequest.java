package com.hrassistant.hrcore.approval;

import jakarta.validation.constraints.Size;

/**
 * Body for approve/reject. The note is optional to approve, required to reject; that rule lives in
 * LeaveRequest.reject, not here, so it holds no matter which endpoint or caller changes the status.
 */
public record DecisionRequest(@Size(max = 500) String note) {
}
