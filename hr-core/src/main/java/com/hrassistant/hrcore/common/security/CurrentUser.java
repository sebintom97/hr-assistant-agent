package com.hrassistant.hrcore.common.security;

import java.util.UUID;

public record CurrentUser(UUID employeeId, UUID tenantId) {
    
}
