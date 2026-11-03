-- Initial schema
CREATE TABLE system_configurations (
    config_key VARCHAR(255) NOT NULL PRIMARY KEY,
    config_value JSON,
    date_modified DATETIME(6) NOT NULL
);

CREATE TABLE auth_provider (
    id UUID NOT NULL PRIMARY KEY,
    name VARCHAR(255) NOT NULL,
    provider_type VARCHAR(255) NOT NULL,
    settings JSON NOT NULL,
    enabled BOOLEAN NOT NULL DEFAULT TRUE,
    date_created DATETIME(6) NOT NULL,
    date_modified DATETIME(6) NOT NULL
);

CREATE TABLE registered_component (
    id VARCHAR(255) NOT NULL PRIMARY KEY,
    component_kind VARCHAR(64) NOT NULL,
    date_created DATETIME(6) NOT NULL,
    date_modified DATETIME(6) NOT NULL
);

CREATE INDEX idx_registered_component_kind ON registered_component (component_kind);

CREATE TABLE list_type (
    id VARCHAR(255) NOT NULL PRIMARY KEY,
    CONSTRAINT fk_list_type_registered_component FOREIGN KEY (id) REFERENCES registered_component (id)
);

CREATE TABLE object_property (
    id VARCHAR(255) NOT NULL PRIMARY KEY,
    type VARCHAR(255) NOT NULL,
    list_type_id VARCHAR(255),
    validator VARCHAR(255),
    security_filter VARCHAR(255),
    default_value JSON,
    system_property BOOLEAN NOT NULL,
    CONSTRAINT fk_object_property_registered_component FOREIGN KEY (id) REFERENCES registered_component (id),
    CONSTRAINT fk_object_property_list_type FOREIGN KEY (list_type_id) REFERENCES list_type (id)
);

CREATE INDEX idx_object_property_list_type_id ON object_property (list_type_id);

CREATE TABLE list_element (
    id VARCHAR(255) NOT NULL PRIMARY KEY,
    parent_id VARCHAR(255) NOT NULL,
    element_index INT NOT NULL,
    active_to DATETIME(6),
    CONSTRAINT fk_list_element_registered_component FOREIGN KEY (id) REFERENCES registered_component (id),
    CONSTRAINT fk_list_element_parent FOREIGN KEY (parent_id) REFERENCES list_type (id)
);

CREATE INDEX idx_list_element_parent_order ON list_element (parent_id, element_index);

CREATE TABLE list_element_alias (
    list_element_id VARCHAR(255) NOT NULL,
    aliases VARCHAR(255),
    CONSTRAINT fk_list_element_alias_element FOREIGN KEY (list_element_id) REFERENCES list_element (id)
);

CREATE INDEX idx_list_element_alias_element ON list_element_alias (list_element_id);

CREATE TABLE managed_object_type (
    id VARCHAR(255) NOT NULL PRIMARY KEY,
    base_type_id VARCHAR(255),
    is_abstract BOOLEAN NOT NULL DEFAULT FALSE,
    CONSTRAINT fk_managed_object_type_registered_component FOREIGN KEY (id) REFERENCES registered_component (id),
    CONSTRAINT fk_managed_object_type_base FOREIGN KEY (base_type_id) REFERENCES managed_object_type (id)
);

CREATE INDEX idx_managed_object_type_base ON managed_object_type (base_type_id);

CREATE TABLE location_type (
    id VARCHAR(255) NOT NULL PRIMARY KEY,
    kind VARCHAR(32),
    CONSTRAINT fk_location_type_holder FOREIGN KEY (id) REFERENCES managed_object_type (id)
);

CREATE TABLE location_type_property (
    location_type VARCHAR(255) NOT NULL,
    property_id VARCHAR(255) NOT NULL,
    default_value JSON,
    CONSTRAINT fk_ltp_location_type FOREIGN KEY (location_type) REFERENCES location_type (id),
    CONSTRAINT fk_ltp_property FOREIGN KEY (property_id) REFERENCES object_property (id)
);

CREATE INDEX idx_ltp_property_id ON location_type_property (property_id);

CREATE TABLE record_type (
    id VARCHAR(255) NOT NULL PRIMARY KEY,
    security_filter VARCHAR(255),
    security_filter_usage SMALLINT NOT NULL CHECK (security_filter_usage BETWEEN 0 AND 2),
    content_types JSON,
    CONSTRAINT fk_record_type_holder FOREIGN KEY (id) REFERENCES managed_object_type (id)
);

CREATE TABLE record_type_property (
    record_type VARCHAR(255) NOT NULL,
    property_id VARCHAR(255) NOT NULL,
    default_value JSON,
    CONSTRAINT fk_rtp_record_type FOREIGN KEY (record_type) REFERENCES record_type (id),
    CONSTRAINT fk_rtp_property FOREIGN KEY (property_id) REFERENCES object_property (id)
);

CREATE INDEX idx_rtp_property_id ON record_type_property (property_id);

CREATE TABLE relationship_type (
    id VARCHAR(255) NOT NULL PRIMARY KEY,
    source_type_id VARCHAR(255) NOT NULL,
    target_type_id VARCHAR(255) NOT NULL,
    unique_per_source BOOLEAN NOT NULL,
    CONSTRAINT fk_relationship_type_registered_component FOREIGN KEY (id) REFERENCES registered_component (id),
    CONSTRAINT fk_relationship_type_source_type FOREIGN KEY (source_type_id) REFERENCES managed_object_type (id),
    CONSTRAINT fk_relationship_type_target_type FOREIGN KEY (target_type_id) REFERENCES managed_object_type (id)
);

CREATE INDEX idx_relationship_type_source_type ON relationship_type (source_type_id);
CREATE INDEX idx_relationship_type_target_type ON relationship_type (target_type_id);

CREATE TABLE managed_object (
    id UUID NOT NULL PRIMARY KEY,
    object_kind VARCHAR(32),
    type_id VARCHAR(255) NOT NULL,
    notes LONGTEXT,
    date_created DATETIME(6) NOT NULL,
    date_modified DATETIME(6) NOT NULL,
    CONSTRAINT fk_managed_object_type FOREIGN KEY (type_id) REFERENCES managed_object_type (id)
);

CREATE INDEX idx_managed_object_kind ON managed_object (object_kind);
CREATE INDEX idx_managed_object_type_id ON managed_object (type_id);

CREATE TABLE location (
    id UUID NOT NULL PRIMARY KEY,
    name VARCHAR(255) NOT NULL UNIQUE,
    CONSTRAINT fk_location_holder FOREIGN KEY (id) REFERENCES managed_object (id)
);

CREATE INDEX idx_location_name ON location (name);

CREATE TABLE user_details (
    id UUID NOT NULL PRIMARY KEY,
    auth_provider_id UUID,
    given_name VARCHAR(255),
    surname VARCHAR(255),
    honorific VARCHAR(255),
    email VARCHAR(255),
    enabled BOOLEAN NOT NULL DEFAULT TRUE,
    session_epoch INT NOT NULL DEFAULT 0,
    CONSTRAINT fk_user_location FOREIGN KEY (id) REFERENCES location (id),
    CONSTRAINT fk_user_auth_provider FOREIGN KEY (auth_provider_id) REFERENCES auth_provider (id)
);

CREATE INDEX idx_user_details_auth_provider_id ON user_details (auth_provider_id);

CREATE TABLE group_details (
    id UUID NOT NULL PRIMARY KEY,
    CONSTRAINT fk_group_location FOREIGN KEY (id) REFERENCES location (id)
);

CREATE TABLE `record` (
    id UUID NOT NULL PRIMARY KEY,
    title VARCHAR(255) NOT NULL,
    keywords LONGTEXT,
    date_registered DATETIME(6),
    CONSTRAINT fk_record_holder FOREIGN KEY (id) REFERENCES managed_object (id)
);

CREATE TABLE managed_object_property_value (
    managed_object_id UUID NOT NULL,
    property_id VARCHAR(255) NOT NULL,
    property_value JSON NOT NULL,
    PRIMARY KEY (managed_object_id, property_id),
    CONSTRAINT fk_mopv_object FOREIGN KEY (managed_object_id) REFERENCES managed_object (id),
    CONSTRAINT fk_mopv_property FOREIGN KEY (property_id) REFERENCES object_property (id)
);

CREATE INDEX idx_mopv_property_id ON managed_object_property_value (property_id);

CREATE TABLE file_store (
    id UUID NOT NULL PRIMARY KEY,
    name VARCHAR(255) NOT NULL,
    type VARCHAR(255) NOT NULL,
    properties JSON NOT NULL,
    date_created DATETIME(6) NOT NULL,
    date_modified DATETIME(6) NOT NULL
);

CREATE TABLE file_store_middleware (
    id UUID NOT NULL PRIMARY KEY,
    name VARCHAR(255) NOT NULL,
    type VARCHAR(255) NOT NULL,
    properties JSON NOT NULL,
    date_created DATETIME(6) NOT NULL,
    date_modified DATETIME(6) NOT NULL
);

CREATE TABLE file_store_middleware_usage (
    file_store_id UUID NOT NULL,
    middleware_id UUID NOT NULL,
    application_order INT,
    CONSTRAINT fk_fsmu_store FOREIGN KEY (file_store_id) REFERENCES file_store (id),
    CONSTRAINT fk_fsmu_middleware FOREIGN KEY (middleware_id) REFERENCES file_store_middleware (id)
);

CREATE INDEX idx_fsmu_file_store_id ON file_store_middleware_usage (file_store_id);

CREATE TABLE file_store_entry (
    id UUID NOT NULL PRIMARY KEY,
    store_id UUID NOT NULL,
    path VARCHAR(255) NOT NULL,
    hash_algorithm VARCHAR(255) NOT NULL,
    hash VARCHAR(255) NOT NULL,
    size_bytes BIGINT NOT NULL,
    extension VARCHAR(255),
    date_created DATETIME(6) NOT NULL,
    CONSTRAINT fk_fse_store FOREIGN KEY (store_id) REFERENCES file_store (id)
);

CREATE INDEX idx_fse_store_id ON file_store_entry (store_id);

CREATE TABLE plugin (
    name VARCHAR(255) NOT NULL PRIMARY KEY,
    version VARCHAR(255) NOT NULL,
    file_id UUID UNIQUE,
    enabled BOOLEAN NOT NULL DEFAULT TRUE,
    date_created TIMESTAMP NOT NULL,
    date_modified TIMESTAMP NOT NULL,
    CONSTRAINT fk_plugin_file FOREIGN KEY (file_id) REFERENCES file_store_entry (id)
);

CREATE TABLE record_revision (
    id UUID NOT NULL PRIMARY KEY,
    version VARCHAR(255) NOT NULL,
    date_created DATETIME(6) NOT NULL,
    record_id UUID NOT NULL,
    file_id UUID NOT NULL UNIQUE,
    CONSTRAINT uk_record_version UNIQUE (record_id, version),
    CONSTRAINT fk_rr_record FOREIGN KEY (record_id) REFERENCES `record` (id),
    CONSTRAINT fk_rr_file FOREIGN KEY (file_id) REFERENCES file_store_entry (id)
);

CREATE TABLE relationship (
    id UUID NOT NULL PRIMARY KEY,
    source_id UUID NOT NULL,
    target_id UUID NOT NULL,
    type_id VARCHAR(255) NOT NULL,
    date_created DATETIME(6) NOT NULL,
    active_to DATETIME(6),
    -- MariaDB has no filtered unique indexes; NULL marker lets ended edges share the same key.
    active_edge_marker CHAR(1) AS (IF(active_to IS NULL, 'Y', NULL)) STORED,
    CONSTRAINT fk_rel_source FOREIGN KEY (source_id) REFERENCES managed_object (id),
    CONSTRAINT fk_rel_target FOREIGN KEY (target_id) REFERENCES managed_object (id),
    CONSTRAINT fk_rel_type FOREIGN KEY (type_id) REFERENCES relationship_type (id),
    UNIQUE KEY uk_rel_active_edge (source_id, target_id, type_id, active_edge_marker)
);

CREATE INDEX idx_rel_source ON relationship (source_id);
CREATE INDEX idx_rel_target ON relationship (target_id);
CREATE INDEX idx_rel_type ON relationship (type_id);

CREATE TABLE audit_event (
    id UUID NOT NULL PRIMARY KEY,
    occurred_at DATETIME(6) NOT NULL,
    actor_id UUID,
    actor_username VARCHAR(255),
    operation VARCHAR(50) NOT NULL,
    target_type VARCHAR(100) NOT NULL,
    target_id VARCHAR(255) NOT NULL,
    action_id VARCHAR(255),
    summary VARCHAR(1000) NOT NULL,
    changes LONGTEXT,
    relationships LONGTEXT,
    comment VARCHAR(2000),
    metadata LONGTEXT
);

CREATE INDEX idx_audit_event_target ON audit_event (target_type, target_id, occurred_at DESC);
CREATE INDEX idx_audit_event_actor ON audit_event (actor_id, occurred_at DESC);

CREATE TABLE audit_policy (
    entity_type VARCHAR(100) NOT NULL,
    operation VARCHAR(50) NOT NULL,
    enabled BOOLEAN NOT NULL,
    requires_comment BOOLEAN NOT NULL,
    date_modified DATETIME(6) NOT NULL,
    PRIMARY KEY (entity_type, operation)
);

CREATE TABLE translation_override (
    message_key VARCHAR(512) NOT NULL,
    locale VARCHAR(32) NOT NULL,
    value TEXT NOT NULL,
    updated_at DATETIME(6) NOT NULL,
    PRIMARY KEY (message_key, locale)
);

CREATE INDEX idx_translation_override_locale ON translation_override (locale);
