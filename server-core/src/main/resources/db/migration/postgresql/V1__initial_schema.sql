-- Initial schema matching current JPA entities (PostgreSQL)
CREATE TABLE system_configurations (
    config_key VARCHAR(255) NOT NULL PRIMARY KEY,
    config_value JSON
);

CREATE TABLE auth_provider (
    id UUID NOT NULL PRIMARY KEY,
    name VARCHAR(255) NOT NULL,
    provider_type VARCHAR(255) NOT NULL,
    settings JSON NOT NULL,
    enabled BOOLEAN NOT NULL DEFAULT TRUE
);

CREATE TABLE list_type (
    id VARCHAR(255) NOT NULL PRIMARY KEY,
    name VARCHAR(255) NOT NULL
);

CREATE TABLE object_property (
    id VARCHAR(255) NOT NULL PRIMARY KEY,
    name VARCHAR(255) NOT NULL,
    description VARCHAR(255) NOT NULL,
    type VARCHAR(255) NOT NULL,
    list_type_id VARCHAR(255),
    validator VARCHAR(255),
    security_filter VARCHAR(255),
    default_value JSON,
    user_hidden BOOLEAN NOT NULL,
    CONSTRAINT fk_object_property_list_type FOREIGN KEY (list_type_id) REFERENCES list_type (id)
);

CREATE INDEX IF NOT EXISTS idx_object_property_list_type_id ON object_property (list_type_id);

CREATE TABLE list_element (
    id VARCHAR(255) NOT NULL PRIMARY KEY,
    parent_id VARCHAR(255) NOT NULL,
    name VARCHAR(255) NOT NULL,
    description VARCHAR(255) NOT NULL,
    element_index INTEGER NOT NULL,
    active_to TIMESTAMPTZ,
    CONSTRAINT fk_list_element_parent FOREIGN KEY (parent_id) REFERENCES list_type (id)
);

CREATE INDEX IF NOT EXISTS idx_list_element_parent_order ON list_element (parent_id, element_index);

CREATE TABLE list_element_alias (
    list_element_id VARCHAR(255) NOT NULL,
    aliases VARCHAR(255),
    CONSTRAINT fk_list_element_alias_element FOREIGN KEY (list_element_id) REFERENCES list_element (id)
);

CREATE INDEX IF NOT EXISTS idx_list_element_alias_element ON list_element_alias (list_element_id);

CREATE TABLE record_type (
    id VARCHAR(255) NOT NULL PRIMARY KEY,
    name VARCHAR(255) NOT NULL,
    description VARCHAR(255) NOT NULL,
    security_filter VARCHAR(255),
    security_filter_usage SMALLINT NOT NULL CHECK (security_filter_usage BETWEEN 0 AND 2),
    content_types JSON
);

CREATE TABLE record_type_property (
    record_type VARCHAR(255) NOT NULL,
    property_id VARCHAR(255) NOT NULL,
    default_value JSON,
    CONSTRAINT fk_rtp_record_type FOREIGN KEY (record_type) REFERENCES record_type (id),
    CONSTRAINT fk_rtp_property FOREIGN KEY (property_id) REFERENCES object_property (id)
);

CREATE INDEX IF NOT EXISTS idx_rtp_property_id ON record_type_property (property_id);

CREATE TABLE user_details (
    id UUID NOT NULL PRIMARY KEY,
    username VARCHAR(255) NOT NULL UNIQUE,
    auth_provider_id UUID,
    date_created TIMESTAMPTZ NOT NULL,
    date_modified TIMESTAMPTZ NOT NULL,
    given_name VARCHAR(255),
    surname VARCHAR(255),
    honorific VARCHAR(255),
    email VARCHAR(255),
    notes VARCHAR(255),
    enabled BOOLEAN NOT NULL DEFAULT TRUE,
    CONSTRAINT fk_user_auth_provider FOREIGN KEY (auth_provider_id) REFERENCES auth_provider (id)
);

CREATE INDEX IF NOT EXISTS idx_user_details_auth_provider_id ON user_details (auth_provider_id);

CREATE TABLE user_property_value (
    id UUID NOT NULL PRIMARY KEY,
    user_id UUID NOT NULL,
    property_id VARCHAR(255) NOT NULL,
    property_value JSON,
    CONSTRAINT fk_upv_user FOREIGN KEY (user_id) REFERENCES user_details (id),
    CONSTRAINT fk_upv_property FOREIGN KEY (property_id) REFERENCES object_property (id)
);

CREATE UNIQUE INDEX IF NOT EXISTS uk_user_property_value_user_property ON user_property_value (user_id, property_id);
CREATE INDEX IF NOT EXISTS idx_upv_property_id ON user_property_value (property_id);

CREATE TABLE auth_token (
    token_value VARCHAR(255) NOT NULL PRIMARY KEY,
    user_id UUID NOT NULL,
    expiry_date TIMESTAMPTZ NOT NULL,
    CONSTRAINT fk_auth_token_user FOREIGN KEY (user_id) REFERENCES user_details (id)
);

CREATE INDEX IF NOT EXISTS idx_auth_token_user_id ON auth_token (user_id);
CREATE INDEX IF NOT EXISTS idx_auth_token_expiry_date ON auth_token (expiry_date);

CREATE TABLE file_store (
    id UUID NOT NULL PRIMARY KEY,
    type VARCHAR(255) NOT NULL,
    properties JSON NOT NULL
);

CREATE TABLE file_store_middleware (
    id UUID NOT NULL PRIMARY KEY,
    type VARCHAR(255) NOT NULL,
    properties JSON NOT NULL
);

CREATE TABLE file_store_middleware_usage (
    file_store_id UUID NOT NULL,
    middleware_id UUID NOT NULL,
    application_order INTEGER,
    CONSTRAINT fk_fsmu_store FOREIGN KEY (file_store_id) REFERENCES file_store (id),
    CONSTRAINT fk_fsmu_middleware FOREIGN KEY (middleware_id) REFERENCES file_store_middleware (id)
);

CREATE INDEX IF NOT EXISTS idx_fsmu_file_store_id ON file_store_middleware_usage (file_store_id);

CREATE TABLE file_store_entry (
    id UUID NOT NULL PRIMARY KEY,
    store_id UUID NOT NULL,
    path VARCHAR(255) NOT NULL,
    hash_algorithm VARCHAR(255) NOT NULL,
    hash VARCHAR(255) NOT NULL,
    size_bytes BIGINT NOT NULL,
    extension VARCHAR(255),
    CONSTRAINT fk_fse_store FOREIGN KEY (store_id) REFERENCES file_store (id)
);

CREATE INDEX IF NOT EXISTS idx_fse_store_id ON file_store_entry (store_id);

CREATE TABLE plugin (
    name VARCHAR(255) NOT NULL PRIMARY KEY,
    version VARCHAR(255) NOT NULL,
    file_id UUID NOT NULL UNIQUE,
    CONSTRAINT fk_plugin_file FOREIGN KEY (file_id) REFERENCES file_store_entry (id)
);

CREATE TABLE "record" (
    id UUID NOT NULL PRIMARY KEY,
    title VARCHAR(255) NOT NULL,
    type_id VARCHAR(255) NOT NULL,
    notes TEXT,
    date_created TIMESTAMPTZ NOT NULL,
    date_registered TIMESTAMPTZ,
    date_modified TIMESTAMPTZ NOT NULL,
    keywords TEXT,
    mime_types JSON,
    CONSTRAINT fk_record_type FOREIGN KEY (type_id) REFERENCES record_type (id)
);

CREATE INDEX IF NOT EXISTS idx_record_type_id ON "record" (type_id);

CREATE TABLE record_property_value (
    id UUID NOT NULL PRIMARY KEY,
    record_id UUID NOT NULL,
    property_id VARCHAR(255) NOT NULL,
    property_value JSON,
    CONSTRAINT fk_rpv_record FOREIGN KEY (record_id) REFERENCES "record" (id),
    CONSTRAINT fk_rpv_property FOREIGN KEY (property_id) REFERENCES object_property (id)
);

CREATE UNIQUE INDEX IF NOT EXISTS uk_record_property_value_record_property ON record_property_value (record_id, property_id);
CREATE INDEX IF NOT EXISTS idx_rpv_property_id ON record_property_value (property_id);

CREATE TABLE record_revision (
    id UUID NOT NULL PRIMARY KEY,
    version VARCHAR(255) NOT NULL,
    created_date TIMESTAMPTZ NOT NULL,
    record_id UUID NOT NULL,
    file_id UUID NOT NULL UNIQUE,
    CONSTRAINT uk_record_version UNIQUE (record_id, version),
    CONSTRAINT fk_rr_record FOREIGN KEY (record_id) REFERENCES "record" (id),
    CONSTRAINT fk_rr_file FOREIGN KEY (file_id) REFERENCES file_store_entry (id)
);

CREATE TABLE audit_event (
    id UUID NOT NULL PRIMARY KEY,
    occurred_at TIMESTAMPTZ NOT NULL,
    actor_id UUID,
    actor_username VARCHAR(255),
    operation VARCHAR(50) NOT NULL,
    target_type VARCHAR(100) NOT NULL,
    target_id VARCHAR(255) NOT NULL,
    action_id VARCHAR(255),
    summary VARCHAR(1000) NOT NULL,
    changes TEXT,
    relationships TEXT,
    comment VARCHAR(2000),
    metadata TEXT
);

CREATE INDEX IF NOT EXISTS idx_audit_event_target ON audit_event (target_type, target_id, occurred_at DESC);
CREATE INDEX IF NOT EXISTS idx_audit_event_actor ON audit_event (actor_id, occurred_at DESC);

CREATE TABLE audit_policy (
    entity_type VARCHAR(100) NOT NULL,
    operation VARCHAR(50) NOT NULL,
    enabled BOOLEAN NOT NULL,
    requires_comment BOOLEAN NOT NULL,
    display_name VARCHAR(255) NOT NULL,
    description VARCHAR(1000),
    PRIMARY KEY (entity_type, operation)
);
