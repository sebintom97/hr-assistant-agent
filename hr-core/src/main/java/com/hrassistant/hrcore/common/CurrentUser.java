package com.hrassistant.hrcore.common;

import com.hrassistant.hrcore.employee.Role;
import java.util.UUID;

/**
 * Who is making this request. Controllers receive it as a method parameter; services use
 * {@code tenantId} to filter every query (rule 5).
 *
 * How it's built is the job of {@link CurrentUserResolver}, the ONLY class that changes in Phase 2.
 */
public record CurrentUser(UUID employeeId, UUID tenantId, Role role) {
}
