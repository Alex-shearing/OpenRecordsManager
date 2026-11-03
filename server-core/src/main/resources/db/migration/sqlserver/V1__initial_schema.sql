-- Initial schema
IF NOT EXISTS (SELECT * FROM sys.tables WHERE name = 'system_configurations')
BEGIN
    CREATE TABLE system_configurations (
        config_key NVARCHAR(255) NOT NULL PRIMARY KEY,
        config_value VARCHAR(MAX),
        date_modified DATETIMEOFFSET NOT NULL
    );
END
GO

CREATE TABLE auth_provider (
    id UNIQUEIDENTIFIER NOT NULL PRIMARY KEY,
    name NVARCHAR(255) NOT NULL,
    provider_type NVARCHAR(255) NOT NULL,
    settings VARCHAR(MAX) NOT NULL,
    enabled BIT NOT NULL CONSTRAINT df_auth_provider_enabled DEFAULT 1,
    date_created DATETIMEOFFSET NOT NULL,
    date_modified DATETIMEOFFSET NOT NULL
);
GO

CREATE TABLE registered_component (
    id NVARCHAR(255) NOT NULL PRIMARY KEY,
    component_kind NVARCHAR(64) NOT NULL,
    date_created DATETIMEOFFSET NOT NULL,
    date_modified DATETIMEOFFSET NOT NULL
);
GO

CREATE INDEX idx_registered_component_kind ON registered_component (component_kind);
GO

CREATE TABLE list_type (
    id NVARCHAR(255) NOT NULL PRIMARY KEY,
    CONSTRAINT fk_list_type_registered_component FOREIGN KEY (id) REFERENCES registered_component (id)
);
GO

CREATE TABLE object_property (
    id NVARCHAR(255) NOT NULL PRIMARY KEY,
    type NVARCHAR(255) NOT NULL,
    list_type_id NVARCHAR(255) NULL,
    validator NVARCHAR(255) NULL,
    security_filter NVARCHAR(255) NULL,
    default_value VARCHAR(MAX) NULL,
    system_property BIT NOT NULL,
    CONSTRAINT fk_object_property_registered_component FOREIGN KEY (id) REFERENCES registered_component (id),
    CONSTRAINT fk_object_property_list_type FOREIGN KEY (list_type_id) REFERENCES list_type (id)
);
GO

CREATE INDEX idx_object_property_list_type_id ON object_property (list_type_id);
GO

CREATE TABLE list_element (
    id NVARCHAR(255) NOT NULL PRIMARY KEY,
    parent_id NVARCHAR(255) NOT NULL,
    element_index INT NOT NULL,
    active_to DATETIMEOFFSET NULL,
    CONSTRAINT fk_list_element_registered_component FOREIGN KEY (id) REFERENCES registered_component (id),
    CONSTRAINT fk_list_element_parent FOREIGN KEY (parent_id) REFERENCES list_type (id)
);
GO

CREATE INDEX idx_list_element_parent_order ON list_element (parent_id, element_index);
GO

CREATE TABLE list_element_alias (
    list_element_id NVARCHAR(255) NOT NULL,
    aliases NVARCHAR(255) NULL,
    CONSTRAINT fk_list_element_alias_element FOREIGN KEY (list_element_id) REFERENCES list_element (id)
);
GO

CREATE INDEX idx_list_element_alias_element ON list_element_alias (list_element_id);
GO

CREATE TABLE managed_object_type (
    id NVARCHAR(255) NOT NULL PRIMARY KEY,
    base_type_id NVARCHAR(255) NULL,
    is_abstract BIT NOT NULL CONSTRAINT df_managed_object_type_is_abstract DEFAULT 0,
    CONSTRAINT fk_managed_object_type_registered_component FOREIGN KEY (id) REFERENCES registered_component (id),
    CONSTRAINT fk_managed_object_type_base FOREIGN KEY (base_type_id) REFERENCES managed_object_type (id)
);
GO

CREATE INDEX idx_managed_object_type_base ON managed_object_type (base_type_id);
GO

CREATE TABLE location_type (
    id NVARCHAR(255) NOT NULL PRIMARY KEY,
    kind NVARCHAR(32) NULL,
    CONSTRAINT fk_location_type_holder FOREIGN KEY (id) REFERENCES managed_object_type (id)
);
GO

CREATE TABLE location_type_property (
    location_type NVARCHAR(255) NOT NULL,
    property_id NVARCHAR(255) NOT NULL,
    default_value VARCHAR(MAX) NULL,
    CONSTRAINT fk_ltp_location_type FOREIGN KEY (location_type) REFERENCES location_type (id),
    CONSTRAINT fk_ltp_property FOREIGN KEY (property_id) REFERENCES object_property (id)
);
GO

CREATE INDEX idx_ltp_property_id ON location_type_property (property_id);
GO

CREATE TABLE record_type (
    id NVARCHAR(255) NOT NULL PRIMARY KEY,
    security_filter NVARCHAR(255) NULL,
    security_filter_usage TINYINT NOT NULL CHECK (security_filter_usage BETWEEN 0 AND 2),
    content_types VARCHAR(MAX) NULL,
    CONSTRAINT fk_record_type_holder FOREIGN KEY (id) REFERENCES managed_object_type (id)
);
GO

CREATE TABLE record_type_property (
    record_type NVARCHAR(255) NOT NULL,
    property_id NVARCHAR(255) NOT NULL,
    default_value VARCHAR(MAX) NULL,
    CONSTRAINT fk_rtp_record_type FOREIGN KEY (record_type) REFERENCES record_type (id),
    CONSTRAINT fk_rtp_property FOREIGN KEY (property_id) REFERENCES object_property (id)
);
GO

CREATE INDEX idx_rtp_property_id ON record_type_property (property_id);
GO

CREATE TABLE relationship_type (
    id NVARCHAR(255) NOT NULL PRIMARY KEY,
    source_type_id NVARCHAR(255) NOT NULL,
    target_type_id NVARCHAR(255) NOT NULL,
    unique_per_source BIT NOT NULL,
    CONSTRAINT fk_relationship_type_registered_component FOREIGN KEY (id) REFERENCES registered_component (id),
    CONSTRAINT fk_relationship_type_source_type FOREIGN KEY (source_type_id) REFERENCES managed_object_type (id),
    CONSTRAINT fk_relationship_type_target_type FOREIGN KEY (target_type_id) REFERENCES managed_object_type (id)
);
GO

CREATE INDEX idx_relationship_type_source_type ON relationship_type (source_type_id);
GO

CREATE INDEX idx_relationship_type_target_type ON relationship_type (target_type_id);
GO

CREATE TABLE managed_object (
    id UNIQUEIDENTIFIER NOT NULL PRIMARY KEY,
    object_kind NVARCHAR(32) NULL,
    type_id NVARCHAR(255) NOT NULL,
    notes VARCHAR(MAX) NULL,
    date_created DATETIMEOFFSET NOT NULL,
    date_modified DATETIMEOFFSET NOT NULL,
    CONSTRAINT fk_managed_object_type FOREIGN KEY (type_id) REFERENCES managed_object_type (id)
);
GO

CREATE INDEX idx_managed_object_kind ON managed_object (object_kind);
GO

CREATE INDEX idx_managed_object_type_id ON managed_object (type_id);
GO

CREATE TABLE location (
    id UNIQUEIDENTIFIER NOT NULL PRIMARY KEY,
    name NVARCHAR(255) NOT NULL UNIQUE,
    CONSTRAINT fk_location_holder FOREIGN KEY (id) REFERENCES managed_object (id)
);
GO


CREATE INDEX idx_location_name ON location (name);
GO

CREATE TABLE user_details (
    id UNIQUEIDENTIFIER NOT NULL PRIMARY KEY,
    auth_provider_id UNIQUEIDENTIFIER NULL,
    given_name NVARCHAR(255) NULL,
    surname NVARCHAR(255) NULL,
    honorific NVARCHAR(255) NULL,
    email NVARCHAR(255) NULL,
    enabled BIT NOT NULL CONSTRAINT df_user_details_enabled DEFAULT 1,
    session_epoch INT NOT NULL CONSTRAINT df_user_details_session_epoch DEFAULT 0,
    CONSTRAINT fk_user_location FOREIGN KEY (id) REFERENCES location (id),
    CONSTRAINT fk_user_auth_provider FOREIGN KEY (auth_provider_id) REFERENCES auth_provider (id)
);
GO

CREATE INDEX idx_user_details_auth_provider_id ON user_details (auth_provider_id);
GO

CREATE TABLE group_details (
    id UNIQUEIDENTIFIER NOT NULL PRIMARY KEY,
    CONSTRAINT fk_group_location FOREIGN KEY (id) REFERENCES location (id)
);
GO

CREATE TABLE record (
    id UNIQUEIDENTIFIER NOT NULL PRIMARY KEY,
    title NVARCHAR(255) NOT NULL,
    keywords VARCHAR(MAX) NULL,
    date_registered DATETIMEOFFSET NULL,
    CONSTRAINT fk_record_holder FOREIGN KEY (id) REFERENCES managed_object (id)
);
GO

CREATE TABLE managed_object_property_value (
    managed_object_id UNIQUEIDENTIFIER NOT NULL,
    property_id NVARCHAR(255) NOT NULL,
    property_value VARCHAR(MAX) NOT NULL,
    PRIMARY KEY (managed_object_id, property_id),
    CONSTRAINT fk_mopv_object FOREIGN KEY (managed_object_id) REFERENCES managed_object (id),
    CONSTRAINT fk_mopv_property FOREIGN KEY (property_id) REFERENCES object_property (id)
);
GO

CREATE INDEX idx_mopv_property_id ON managed_object_property_value (property_id);
GO

CREATE TABLE file_store (
    id UNIQUEIDENTIFIER NOT NULL PRIMARY KEY,
    name NVARCHAR(255) NOT NULL,
    type NVARCHAR(255) NOT NULL,
    properties VARCHAR(MAX) NOT NULL,
    date_created DATETIMEOFFSET NOT NULL,
    date_modified DATETIMEOFFSET NOT NULL
);
GO

CREATE TABLE file_store_middleware (
    id UNIQUEIDENTIFIER NOT NULL PRIMARY KEY,
    name NVARCHAR(255) NOT NULL,
    type NVARCHAR(255) NOT NULL,
    properties VARCHAR(MAX) NOT NULL,
    date_created DATETIMEOFFSET NOT NULL,
    date_modified DATETIMEOFFSET NOT NULL
);
GO

CREATE TABLE file_store_middleware_usage (
    file_store_id UNIQUEIDENTIFIER NOT NULL,
    middleware_id UNIQUEIDENTIFIER NOT NULL,
    application_order INT NULL,
    CONSTRAINT fk_fsmu_store FOREIGN KEY (file_store_id) REFERENCES file_store (id),
    CONSTRAINT fk_fsmu_middleware FOREIGN KEY (middleware_id) REFERENCES file_store_middleware (id)
);
GO

CREATE INDEX idx_fsmu_file_store_id ON file_store_middleware_usage (file_store_id);
GO

CREATE TABLE file_store_entry (
    id UNIQUEIDENTIFIER NOT NULL PRIMARY KEY,
    store_id UNIQUEIDENTIFIER NOT NULL,
    path NVARCHAR(255) NOT NULL,
    hash_algorithm NVARCHAR(255) NOT NULL,
    hash NVARCHAR(255) NOT NULL,
    size_bytes BIGINT NOT NULL,
    extension NVARCHAR(255) NULL,
    date_created DATETIMEOFFSET NOT NULL,
    CONSTRAINT fk_fse_store FOREIGN KEY (store_id) REFERENCES file_store (id)
);
GO

CREATE INDEX idx_fse_store_id ON file_store_entry (store_id);
GO

CREATE TABLE plugin (
    name NVARCHAR(255) NOT NULL PRIMARY KEY,
    version NVARCHAR(255) NOT NULL,
    file_id UNIQUEIDENTIFIER NULL,
    enabled BIT NOT NULL CONSTRAINT df_plugin_enabled DEFAULT 1,
    date_created DATETIMEOFFSET NOT NULL,
    date_modified DATETIMEOFFSET NOT NULL,
    CONSTRAINT fk_plugin_file FOREIGN KEY (file_id) REFERENCES file_store_entry (id)
);
GO

-- SQL Server UNIQUE constraints allow only one NULL; plugins without a stored file need many NULLs.
CREATE UNIQUE INDEX uq_plugin_file_id ON plugin (file_id) WHERE file_id IS NOT NULL;
GO

CREATE TABLE record_revision (
    id UNIQUEIDENTIFIER NOT NULL PRIMARY KEY,
    version NVARCHAR(255) NOT NULL,
    date_created DATETIMEOFFSET NOT NULL,
    record_id UNIQUEIDENTIFIER NOT NULL,
    file_id UNIQUEIDENTIFIER NOT NULL UNIQUE,
    CONSTRAINT uk_record_version UNIQUE (record_id, version),
    CONSTRAINT fk_rr_record FOREIGN KEY (record_id) REFERENCES record (id),
    CONSTRAINT fk_rr_file FOREIGN KEY (file_id) REFERENCES file_store_entry (id)
);
GO

CREATE TABLE relationship (
    id UNIQUEIDENTIFIER NOT NULL PRIMARY KEY,
    source_id UNIQUEIDENTIFIER NOT NULL,
    target_id UNIQUEIDENTIFIER NOT NULL,
    type_id NVARCHAR(255) NOT NULL,
    date_created DATETIMEOFFSET NOT NULL,
    active_to DATETIMEOFFSET NULL,
    CONSTRAINT fk_rel_source FOREIGN KEY (source_id) REFERENCES managed_object (id),
    CONSTRAINT fk_rel_target FOREIGN KEY (target_id) REFERENCES managed_object (id),
    CONSTRAINT fk_rel_type FOREIGN KEY (type_id) REFERENCES relationship_type (id)
);
GO

CREATE INDEX idx_rel_source ON relationship (source_id);
GO

CREATE INDEX idx_rel_target ON relationship (target_id);
GO

CREATE INDEX idx_rel_type ON relationship (type_id);
GO

CREATE UNIQUE INDEX uk_rel_active_edge ON relationship (source_id, target_id, type_id) WHERE active_to IS NULL;
GO

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
    changes VARCHAR(MAX) NULL,
    relationships VARCHAR(MAX) NULL,
    comment NVARCHAR(2000) NULL,
    metadata VARCHAR(MAX) NULL
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
    date_modified DATETIMEOFFSET NOT NULL,
    PRIMARY KEY (entity_type, operation)
);
GO

CREATE TABLE translation_override (
    message_key NVARCHAR(512) NOT NULL,
    locale NVARCHAR(32) NOT NULL,
    value NVARCHAR(MAX) NOT NULL,
    updated_at DATETIMEOFFSET NOT NULL,
    PRIMARY KEY (message_key, locale)
);
GO

CREATE INDEX idx_translation_override_locale ON translation_override (locale);
GO
