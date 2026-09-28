-- V3: audit log (rule 8). Append only: the application INSERTs, never UPDATEs or DELETEs.
--
-- Deliberately no foreign keys to the entity being audited: entity_type + entity_id can point at
-- any table, and an audit row must survive even if what it describes is later removed.

CREATE TABLE audit_log (
    id          uuid        PRIMARY KEY DEFAULT uuidv7(),
    tenant_id   uuid        NOT NULL REFERENCES tenant (id),
    occurred_at timestamptz NOT NULL DEFAULT now(),

    -- Who did it. For AGENT, actor_id is the user the agent acted for (it always uses their JWT).
    actor_type  text        NOT NULL,
    actor_id    uuid,

    action      text        NOT NULL,              -- e.g. LEAVE_REQUEST_APPROVED, AGENT_TOOL_CALL, PERMISSION_DENIED
    entity_type text,                              -- e.g. LEAVE_REQUEST
    entity_id   uuid,
    details     jsonb       NOT NULL DEFAULT '{}'::jsonb,
    request_id  text,                              -- X-Request-Id, to trace one request across both services

    CONSTRAINT audit_log_actor_type_valid CHECK (actor_type IN ('USER', 'AGENT', 'SYSTEM')),
    CONSTRAINT audit_log_actor_present    CHECK (actor_type = 'SYSTEM' OR actor_id IS NOT NULL),
    CONSTRAINT audit_log_actor_same_tenant
        FOREIGN KEY (tenant_id, actor_id) REFERENCES employee (tenant_id, id)
);

CREATE INDEX audit_log_tenant_time_idx   ON audit_log (tenant_id, occurred_at DESC);
CREATE INDEX audit_log_tenant_entity_idx ON audit_log (tenant_id, entity_type, entity_id);
