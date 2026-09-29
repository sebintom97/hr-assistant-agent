package com.hrassistant.hrcore.leave;

import java.math.BigDecimal;

/**
 * Derived, never stored (ADR 004).
 *
 * @param remaining allowance - approved
 * @param available remaining - pending: what can still be requested. Submit is checked against this,
 *                  so pending requests can't add up to more than the employee has.
 */
public record LeaveBalance(
        int year,
        BigDecimal allowance,
        BigDecimal approved,
        BigDecimal pending,
        BigDecimal remaining,
        BigDecimal available) {
}
