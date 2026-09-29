package com.hrassistant.hrcore.leave;

import java.time.LocalDate;
import java.util.UUID;

/**
 * "Who is off when": only name and dates. No reason, leave type or balance, because a colleague
 * may see THAT someone is away, not why.
 */
public record TeamAbsence(UUID employeeId, String employeeName, LocalDate startDate, LocalDate endDate) {
}
