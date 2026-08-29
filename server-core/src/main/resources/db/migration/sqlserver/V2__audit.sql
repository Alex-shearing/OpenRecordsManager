CREATE TABLE audit_event (
    id UNIQUEIDENTIFIER NOT NULL PRIMARY KEY,
    occurred_at DATETIMEOFFSET NOT NULL,
    actor_id UNIQUEIDENTIFIER NULL,
    actor_username NVARCHAR(255) NULL,
    operation NVARCHAR(50) NOT NULL,
    target_type NVARCHAR(100) NOT NULL,
    target_id NVARCHAR(255) NOT NULL,
    action_id NVARCHAR(255) NULL,
    summary NVARCHAR(1000) NOT NULL,
    changes NVARCHAR(MAX) NULL,
    relationships NVARCHAR(MAX) NULL,
    comment NVARCHAR(2000) NULL,
    metadata NVARCHAR(MAX) NULL
);
GO

CREATE INDEX idx_audit_event_target ON audit_event (target_type, target_id, occurred_at DESC);
GO

CREATE INDEX idx_audit_event_actor ON audit_event (actor_id, occurred_at DESC);
GO

CREATE TABLE audit_policy (
    entity_type NVARCHAR(100) NOT NULL,
    operation NVARCHAR(50) NOT NULL,
    enabled BIT NOT NULL,
    requires_comment BIT NOT NULL,
    display_name NVARCHAR(255) NOT NULL,
    description NVARCHAR(1000) NULL,
    PRIMARY KEY (entity_type, operation)
);
GO
