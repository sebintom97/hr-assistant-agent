package com.hrassistant.hrcore.audit;

/** Matches the CHECK constraint audit_log_actor_type_valid. */
public enum ActorType {
    USER,
    AGENT,   // Phase 4: the agent acting with a user's JWT; actor_id is that user
    SYSTEM   // Phase 5: the watcher
}
