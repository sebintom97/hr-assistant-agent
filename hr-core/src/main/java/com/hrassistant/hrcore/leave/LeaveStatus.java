package com.hrassistant.hrcore.leave;

/**
 * Matches the CHECK constraint leave_request_status_valid.
 *
 *            submit
 *   (none) ─────────▶ PENDING ──approve──▶ APPROVED ──cancel (before start)──▶ CANCELLED
 *                        │ ├───reject───▶ REJECTED
 *                        │ └───cancel───▶ CANCELLED
 */
public enum LeaveStatus {
    PENDING,
    APPROVED,
    REJECTED,
    CANCELLED
}
