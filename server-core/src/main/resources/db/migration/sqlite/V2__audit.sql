CREATE TABLE audit_event (
    id BLOB NOT NULL PRIMARY KEY,
    occurred_at TIMESTAMP NOT NULL,
    actor_id BLOB,
    actor_username VARCHAR(255),
    operation VARCHAR(50) NOT NULL,
    target_type VARCHAR(100) NOT NULL,
    target_id VARCHAR(255) NOT NULL,
    action_id VARCHAR(255),
    summary VARCHAR(1000) NOT NULL,
    changes CLOB,
    relationships CLOB,
    comment VARCHAR(2000),
    metadata CLOB
);

CREATE INDEX idx_audit_event_target ON audit_event (target_type, target_id, occurred_at DESC);
CREATE INDEX idx_audit_event_actor ON audit_event (actor_id, occurred_at DESC);

CREATE TABLE audit_policy (
    entity_type VARCHAR(100) NOT NULL,
    operation VARCHAR(50) NOT NULL,
    enabled BOOLEAN NOT NULL,
    requires_comment BOOLEAN NOT NULL,
    display_name VARCHAR(255) NOT NULL,
    description VARCHAR(1000),
    PRIMARY KEY (entity_type, operation)
);
