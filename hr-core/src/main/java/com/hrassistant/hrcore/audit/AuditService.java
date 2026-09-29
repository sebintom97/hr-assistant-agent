package com.hrassistant.hrcore.audit;

import com.hrassistant.hrcore.common.RequestIdFilter;
import jakarta.persistence.EntityManager;
import java.time.Clock;
import java.util.Map;
import java.util.UUID;
import org.slf4j.MDC;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Propagation;
import org.springframework.transaction.annotation.Transactional;

/**
 * Writes audit_log rows (rule 8).
 *
 * Propagation.MANDATORY: this must be called INSIDE the caller's transaction, and fails loudly if
 * there is none. That's what makes "change + audit row" all or nothing: if the audit insert fails,
 * the change rolls back too, and a change can never be committed without its audit row.
 */
@Service
public class AuditService {

    private final EntityManager entityManager;
    private final Clock clock;

    AuditService(EntityManager entityManager, Clock clock) {
        this.entityManager = entityManager;
        this.clock = clock;
    }

    @Transactional(propagation = Propagation.MANDATORY)
    public void record(UUID tenantId, ActorType actorType, UUID actorId, String action,
                       String entityType, UUID entityId, Map<String, Object> details) {
        entityManager.persist(new AuditLog(tenantId, clock.instant(), actorType, actorId, action,
                entityType, entityId, details, MDC.get(RequestIdFilter.MDC_KEY)));
    }
}
