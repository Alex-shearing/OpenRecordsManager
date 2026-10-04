-- Initial schema matching current JPA entities (SQLite)
CREATE TABLE IF NOT EXISTS system_configurations (
    config_key VARCHAR(255) NOT NULL PRIMARY KEY,
    config_value CLOB,
    date_modified TIMESTAMP NOT NULL
);

CREATE TABLE auth_provider (
    id BLOB NOT NULL PRIMARY KEY,
    name VARCHAR(255) NOT NULL,
    provider_type VARCHAR(255) NOT NULL,
    settings CLOB NOT NULL,
    enabled BOOLEAN NOT NULL DEFAULT TRUE,
    date_created TIMESTAMP NOT NULL,
    date_modified TIMESTAMP NOT NULL
);

CREATE TABLE list_type (
    id VARCHAR(255) NOT NULL PRIMARY KEY,
    date_created TIMESTAMP NOT NULL,
    date_modified TIMESTAMP NOT NULL
);

CREATE TABLE object_property (
    id VARCHAR(255) NOT NULL PRIMARY KEY,
    type VARCHAR(255) NOT NULL,
    list_type_id VARCHAR(255),
    validator VARCHAR(255),
    security_filter VARCHAR(255),
    default_value CLOB,
    user_hidden BOOLEAN NOT NULL,
    date_created TIMESTAMP NOT NULL,
    date_modified TIMESTAMP NOT NULL,
    CONSTRAINT fk_object_property_list_type FOREIGN KEY (list_type_id) REFERENCES list_type (id)
);

CREATE INDEX IF NOT EXISTS idx_object_property_list_type_id ON object_property (list_type_id);

CREATE TABLE list_element (
    id VARCHAR(255) NOT NULL PRIMARY KEY,
    parent_id VARCHAR(255) NOT NULL,
    element_index INTEGER NOT NULL,
    active_to TIMESTAMP,
    date_created TIMESTAMP NOT NULL,
    date_modified TIMESTAMP NOT NULL,
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
    security_filter VARCHAR(255),
    security_filter_usage TINYINT NOT NULL CHECK (security_filter_usage BETWEEN 0 AND 2),
    content_types CLOB,
    date_created TIMESTAMP NOT NULL,
    date_modified TIMESTAMP NOT NULL
);

CREATE TABLE record_type_property (
    record_type VARCHAR(255) NOT NULL,
    property_id VARCHAR(255) NOT NULL,
    default_value CLOB,
    CONSTRAINT fk_rtp_record_type FOREIGN KEY (record_type) REFERENCES record_type (id),
    CONSTRAINT fk_rtp_property FOREIGN KEY (property_id) REFERENCES object_property (id)
);

CREATE INDEX IF NOT EXISTS idx_rtp_property_id ON record_type_property (property_id);

CREATE TABLE location_type (
    id VARCHAR(255) NOT NULL PRIMARY KEY,
    kind VARCHAR(32) NOT NULL,
    date_created TIMESTAMP NOT NULL,
    date_modified TIMESTAMP NOT NULL
);

CREATE TABLE location_type_property (
    location_type VARCHAR(255) NOT NULL,
    property_id VARCHAR(255) NOT NULL,
    default_value CLOB,
    CONSTRAINT fk_ltp_location_type FOREIGN KEY (location_type) REFERENCES location_type (id),
    CONSTRAINT fk_ltp_property FOREIGN KEY (property_id) REFERENCES object_property (id)
);

CREATE INDEX IF NOT EXISTS idx_ltp_property_id ON location_type_property (property_id);

CREATE TABLE location (
    id BLOB NOT NULL PRIMARY KEY,
    name VARCHAR(255) NOT NULL,
    notes VARCHAR(255),
    date_created TIMESTAMP NOT NULL,
    date_modified TIMESTAMP NOT NULL,
    location_kind VARCHAR(32) NOT NULL,
    type_id VARCHAR(255) NOT NULL,
    CONSTRAINT fk_location_type FOREIGN KEY (type_id) REFERENCES location_type (id)
);

CREATE INDEX IF NOT EXISTS idx_location_kind ON location (location_kind);
CREATE INDEX IF NOT EXISTS idx_location_name ON location (name);
CREATE INDEX IF NOT EXISTS idx_location_type_id ON location (type_id);

CREATE TABLE user_details (
    id BLOB NOT NULL PRIMARY KEY,
    username VARCHAR(255) NOT NULL UNIQUE,
    auth_provider_id BLOB,
    given_name VARCHAR(255),
    surname VARCHAR(255),
    honorific VARCHAR(255),
    email VARCHAR(255),
    enabled BOOLEAN NOT NULL DEFAULT TRUE,
    session_epoch INTEGER NOT NULL DEFAULT 0,
    CONSTRAINT fk_user_location FOREIGN KEY (id) REFERENCES location (id),
    CONSTRAINT fk_user_auth_provider FOREIGN KEY (auth_provider_id) REFERENCES auth_provider (id)
);

CREATE INDEX IF NOT EXISTS idx_user_details_auth_provider_id ON user_details (auth_provider_id);

CREATE TABLE group_details (
    id BLOB NOT NULL PRIMARY KEY,
    CONSTRAINT fk_group_location FOREIGN KEY (id) REFERENCES location (id)
);

CREATE TABLE location_property_value (
    location_id BLOB NOT NULL,
    property_id VARCHAR(255) NOT NULL,
    property_value CLOB,
    PRIMARY KEY (location_id, property_id),
    CONSTRAINT fk_lpv_location FOREIGN KEY (location_id) REFERENCES location (id),
    CONSTRAINT fk_lpv_property FOREIGN KEY (property_id) REFERENCES object_property (id)
);

CREATE INDEX IF NOT EXISTS idx_lpv_property_id ON location_property_value (property_id);

CREATE TABLE location_relationship_type (
    id VARCHAR(255) NOT NULL PRIMARY KEY,
    source_kind VARCHAR(32) NOT NULL,
    target_kind VARCHAR(32) NOT NULL,
    unique_per_source BOOLEAN NOT NULL,
    date_created TIMESTAMP NOT NULL,
    date_modified TIMESTAMP NOT NULL
);

CREATE TABLE location_relationship (
    id BLOB NOT NULL PRIMARY KEY,
    source_id BLOB NOT NULL,
    target_id BLOB NOT NULL,
    type_id VARCHAR(255) NOT NULL,
    date_created TIMESTAMP NOT NULL,
    active_to TIMESTAMP,
    CONSTRAINT fk_lr_source FOREIGN KEY (source_id) REFERENCES location (id),
    CONSTRAINT fk_lr_target FOREIGN KEY (target_id) REFERENCES location (id),
    CONSTRAINT fk_lr_type FOREIGN KEY (type_id) REFERENCES location_relationship_type (id)
);

CREATE INDEX IF NOT EXISTS idx_lr_source ON location_relationship (source_id);
CREATE INDEX IF NOT EXISTS idx_lr_target ON location_relationship (target_id);
CREATE INDEX IF NOT EXISTS idx_lr_type ON location_relationship (type_id);
CREATE UNIQUE INDEX IF NOT EXISTS uk_lr_active_edge ON location_relationship (source_id, target_id, type_id)
    WHERE active_to IS NULL;

CREATE TABLE file_store (
    id BLOB NOT NULL PRIMARY KEY,
    name VARCHAR(255) NOT NULL,
    type VARCHAR(255) NOT NULL,
    properties CLOB NOT NULL,
    date_created TIMESTAMP NOT NULL,
    date_modified TIMESTAMP NOT NULL
);

CREATE TABLE file_store_middleware (
    id BLOB NOT NULL PRIMARY KEY,
    name VARCHAR(255) NOT NULL,
    type VARCHAR(255) NOT NULL,
    properties CLOB NOT NULL,
    date_created TIMESTAMP NOT NULL,
    date_modified TIMESTAMP NOT NULL
);

CREATE TABLE file_store_middleware_usage (
    file_store_id BLOB NOT NULL,
    middleware_id BLOB NOT NULL,
    application_order INTEGER,
    CONSTRAINT fk_fsmu_store FOREIGN KEY (file_store_id) REFERENCES file_store (id),
    CONSTRAINT fk_fsmu_middleware FOREIGN KEY (middleware_id) REFERENCES file_store_middleware (id)
);

CREATE INDEX IF NOT EXISTS idx_fsmu_file_store_id ON file_store_middleware_usage (file_store_id);

CREATE TABLE file_store_entry (
    id BLOB NOT NULL PRIMARY KEY,
    store_id BLOB NOT NULL,
    path VARCHAR(255) NOT NULL,
    hash_algorithm VARCHAR(255) NOT NULL,
    hash VARCHAR(255) NOT NULL,
    size_bytes BIGINT NOT NULL,
    extension VARCHAR(255),
    date_created TIMESTAMP NOT NULL,
    CONSTRAINT fk_fse_store FOREIGN KEY (store_id) REFERENCES file_store (id)
);

CREATE INDEX IF NOT EXISTS idx_fse_store_id ON file_store_entry (store_id);

CREATE TABLE plugin (
    name VARCHAR(255) NOT NULL PRIMARY KEY,
    version VARCHAR(255) NOT NULL,
    file_id BLOB UNIQUE,
    enabled BOOLEAN NOT NULL DEFAULT TRUE,
    date_created TIMESTAMP NOT NULL,
    date_modified TIMESTAMP NOT NULL,
    CONSTRAINT fk_plugin_file FOREIGN KEY (file_id) REFERENCES file_store_entry (id)
);

CREATE TABLE record (
    id BLOB NOT NULL PRIMARY KEY,
    title VARCHAR(255) NOT NULL,
    type_id VARCHAR(255) NOT NULL,
    notes CLOB,
    date_created TIMESTAMP NOT NULL,
    date_registered TIMESTAMP,
    date_modified TIMESTAMP NOT NULL,
    keywords CLOB,
    mime_types CLOB,
    CONSTRAINT fk_record_type FOREIGN KEY (type_id) REFERENCES record_type (id)
);

CREATE INDEX IF NOT EXISTS idx_record_type_id ON record (type_id);

CREATE TABLE record_property_value (
    record_id BLOB NOT NULL,
    property_id VARCHAR(255) NOT NULL,
    property_value CLOB,
    PRIMARY KEY (record_id, property_id),
    CONSTRAINT fk_rpv_record FOREIGN KEY (record_id) REFERENCES record (id),
    CONSTRAINT fk_rpv_property FOREIGN KEY (property_id) REFERENCES object_property (id)
);

CREATE INDEX IF NOT EXISTS idx_rpv_property_id ON record_property_value (property_id);

CREATE TABLE record_revision (
    id BLOB NOT NULL PRIMARY KEY,
    version VARCHAR(255) NOT NULL,
    date_created TIMESTAMP NOT NULL,
    record_id BLOB NOT NULL,
    file_id BLOB NOT NULL UNIQUE,
    CONSTRAINT uk_record_version UNIQUE (record_id, version),
    CONSTRAINT fk_rr_record FOREIGN KEY (record_id) REFERENCES record (id),
    CONSTRAINT fk_rr_file FOREIGN KEY (file_id) REFERENCES file_store_entry (id)
);

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

CREATE INDEX IF NOT EXISTS idx_audit_event_target ON audit_event (target_type, target_id, occurred_at DESC);
CREATE INDEX IF NOT EXISTS idx_audit_event_actor ON audit_event (actor_id, occurred_at DESC);

CREATE TABLE audit_policy (
    entity_type VARCHAR(100) NOT NULL,
    operation VARCHAR(50) NOT NULL,
    enabled BOOLEAN NOT NULL,
    requires_comment BOOLEAN NOT NULL,
    date_modified TIMESTAMP NOT NULL,
    PRIMARY KEY (entity_type, operation)
);

CREATE TABLE translation_override (
    message_key VARCHAR(512) NOT NULL,
    locale VARCHAR(32) NOT NULL,
    value TEXT NOT NULL,
    updated_at TIMESTAMP NOT NULL,
    PRIMARY KEY (message_key, locale)
);

CREATE INDEX idx_translation_override_locale ON translation_override (locale);
