package com.hrassistant.hrcore.audit;

import com.hrassistant.hrcore.common.Ids;
import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.EnumType;
import jakarta.persistence.Enumerated;
import jakarta.persistence.Id;
import jakarta.persistence.Table;
import java.time.Instant;
import java.util.Map;
import java.util.UUID;
import org.hibernate.annotations.Immutable;
import org.hibernate.annotations.JdbcTypeCode;
import org.hibernate.type.SqlTypes;

/** One audit row. @Immutable: Hibernate will never issue an UPDATE for it (append only). */
@Entity
@Immutable
@Table(name = "audit_log")
class AuditLog {

    @Id
    private UUID id;

    @Column(name = "tenant_id")
    private UUID tenantId;

    @Column(name = "occurred_at")
    private Instant occurredAt;

    @Enumerated(EnumType.STRING)
    @Column(name = "actor_type")
    private ActorType actorType;

    @Column(name = "actor_id")
    private UUID actorId;

    private String action;

    @Column(name = "entity_type")
    private String entityType;

    @Column(name = "entity_id")
    private UUID entityId;

    @JdbcTypeCode(SqlTypes.JSON)    // Map <-> jsonb, serialised with Jackson
    private Map<String, Object> details;

    @Column(name = "request_id")
    private String requestId;

    protected AuditLog() {
        // for JPA
    }

    AuditLog(UUID tenantId, Instant occurredAt, ActorType actorType, UUID actorId, String action,
             String entityType, UUID entityId, Map<String, Object> details, String requestId) {
        this.id = Ids.newId();
        this.tenantId = tenantId;
        this.occurredAt = occurredAt;
        this.actorType = actorType;
        this.actorId = actorId;
        this.action = action;
        this.entityType = entityType;
        this.entityId = entityId;
        this.details = details;
        this.requestId = requestId;
    }
}
