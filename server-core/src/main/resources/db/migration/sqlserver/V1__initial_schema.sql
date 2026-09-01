-- Initial schema matching current JPA entities (SQL Server)
IF NOT EXISTS (SELECT * FROM sys.tables WHERE name = 'system_configurations')
BEGIN
    CREATE TABLE system_configurations (
        config_key NVARCHAR(255) NOT NULL PRIMARY KEY,
        config_value VARCHAR(MAX)
    );
END
GO

CREATE TABLE auth_provider (
    id UNIQUEIDENTIFIER NOT NULL PRIMARY KEY,
    name NVARCHAR(255) NOT NULL,
    provider_type NVARCHAR(255) NOT NULL,
    settings VARCHAR(MAX) NOT NULL,
    enabled BIT NOT NULL CONSTRAINT df_auth_provider_enabled DEFAULT 1
);
GO

CREATE TABLE list_type (
    id NVARCHAR(255) NOT NULL PRIMARY KEY,
    name NVARCHAR(255) NOT NULL
);
GO

CREATE TABLE object_property (
    id NVARCHAR(255) NOT NULL PRIMARY KEY,
    name NVARCHAR(255) NOT NULL,
    description NVARCHAR(255) NOT NULL,
    type NVARCHAR(255) NOT NULL,
    list_type_id NVARCHAR(255) NULL,
    validator NVARCHAR(255) NULL,
    security_filter NVARCHAR(255) NULL,
    default_value VARCHAR(MAX) NULL,
    user_hidden BIT NOT NULL,
    CONSTRAINT fk_object_property_list_type FOREIGN KEY (list_type_id) REFERENCES list_type (id)
);
GO

CREATE INDEX idx_object_property_list_type_id ON object_property (list_type_id);
GO

CREATE TABLE list_element (
    id NVARCHAR(255) NOT NULL PRIMARY KEY,
    parent_id NVARCHAR(255) NOT NULL,
    name NVARCHAR(255) NOT NULL,
    description NVARCHAR(255) NOT NULL,
    element_index INT NOT NULL,
    active_to DATETIMEOFFSET NULL,
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

CREATE TABLE record_type (
    id NVARCHAR(255) NOT NULL PRIMARY KEY,
    name NVARCHAR(255) NOT NULL,
    description NVARCHAR(255) NOT NULL,
    security_filter NVARCHAR(255) NULL,
    security_filter_usage TINYINT NOT NULL CHECK (security_filter_usage BETWEEN 0 AND 2),
    content_types VARCHAR(MAX) NULL
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

CREATE TABLE user_details (
    id UNIQUEIDENTIFIER NOT NULL PRIMARY KEY,
    username NVARCHAR(255) NOT NULL UNIQUE,
    auth_provider_id UNIQUEIDENTIFIER NULL,
    date_created DATETIMEOFFSET NOT NULL,
    date_modified DATETIMEOFFSET NOT NULL,
    given_name NVARCHAR(255) NULL,
    surname NVARCHAR(255) NULL,
    honorific NVARCHAR(255) NULL,
    email NVARCHAR(255) NULL,
    notes NVARCHAR(255) NULL,
    enabled BIT NOT NULL CONSTRAINT df_user_details_enabled DEFAULT 1,
    CONSTRAINT fk_user_auth_provider FOREIGN KEY (auth_provider_id) REFERENCES auth_provider (id)
);
GO

CREATE INDEX idx_user_details_auth_provider_id ON user_details (auth_provider_id);
GO

CREATE TABLE user_property_value (
    id UNIQUEIDENTIFIER NOT NULL PRIMARY KEY,
    user_id UNIQUEIDENTIFIER NOT NULL,
    property_id NVARCHAR(255) NOT NULL,
    property_value VARCHAR(MAX) NULL,
    CONSTRAINT fk_upv_user FOREIGN KEY (user_id) REFERENCES user_details (id),
    CONSTRAINT fk_upv_property FOREIGN KEY (property_id) REFERENCES object_property (id)
);
GO

CREATE UNIQUE INDEX uk_user_property_value_user_property ON user_property_value (user_id, property_id);
GO

CREATE INDEX idx_upv_property_id ON user_property_value (property_id);
GO

CREATE TABLE auth_token (
    token_value NVARCHAR(255) NOT NULL PRIMARY KEY,
    user_id UNIQUEIDENTIFIER NOT NULL,
    expiry_date DATETIMEOFFSET NOT NULL,
    CONSTRAINT fk_auth_token_user FOREIGN KEY (user_id) REFERENCES user_details (id)
);
GO

CREATE INDEX idx_auth_token_user_id ON auth_token (user_id);
GO

CREATE INDEX idx_auth_token_expiry_date ON auth_token (expiry_date);
GO

CREATE TABLE file_store (
    id UNIQUEIDENTIFIER NOT NULL PRIMARY KEY,
    type NVARCHAR(255) NOT NULL,
    properties VARCHAR(MAX) NOT NULL
);
GO

CREATE TABLE file_store_middleware (
    id UNIQUEIDENTIFIER NOT NULL PRIMARY KEY,
    type NVARCHAR(255) NOT NULL,
    properties VARCHAR(MAX) NOT NULL
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
    CONSTRAINT fk_fse_store FOREIGN KEY (store_id) REFERENCES file_store (id)
);
GO

CREATE INDEX idx_fse_store_id ON file_store_entry (store_id);
GO

CREATE TABLE plugin (
    name NVARCHAR(255) NOT NULL PRIMARY KEY,
    version NVARCHAR(255) NOT NULL,
    file_id UNIQUEIDENTIFIER UNIQUE,
    enabled BIT NOT NULL CONSTRAINT df_plugin_enabled DEFAULT 1,
    date_created DATETIMEOFFSET NOT NULL,
    date_modified DATETIMEOFFSET NOT NULL,
    CONSTRAINT fk_plugin_file FOREIGN KEY (file_id) REFERENCES file_store_entry (id)
);
GO

CREATE TABLE record (
    id UNIQUEIDENTIFIER NOT NULL PRIMARY KEY,
    title NVARCHAR(255) NOT NULL,
    type_id NVARCHAR(255) NOT NULL,
    notes VARCHAR(MAX) NULL,
    date_created DATETIMEOFFSET NOT NULL,
    date_registered DATETIMEOFFSET NULL,
    date_modified DATETIMEOFFSET NOT NULL,
    keywords VARCHAR(MAX) NULL,
    mime_types VARCHAR(MAX) NULL,
    CONSTRAINT fk_record_type FOREIGN KEY (type_id) REFERENCES record_type (id)
);
GO

CREATE INDEX idx_record_type_id ON record (type_id);
GO

CREATE TABLE record_property_value (
    id UNIQUEIDENTIFIER NOT NULL PRIMARY KEY,
    record_id UNIQUEIDENTIFIER NOT NULL,
    property_id NVARCHAR(255) NOT NULL,
    property_value VARCHAR(MAX) NULL,
    CONSTRAINT fk_rpv_record FOREIGN KEY (record_id) REFERENCES record (id),
    CONSTRAINT fk_rpv_property FOREIGN KEY (property_id) REFERENCES object_property (id)
);
GO

CREATE UNIQUE INDEX uk_record_property_value_record_property ON record_property_value (record_id, property_id);
GO

CREATE INDEX idx_rpv_property_id ON record_property_value (property_id);
GO

CREATE TABLE record_revision (
    id UNIQUEIDENTIFIER NOT NULL PRIMARY KEY,
    version NVARCHAR(255) NOT NULL,
    created_date DATETIMEOFFSET NOT NULL,
    record_id UNIQUEIDENTIFIER NOT NULL,
    file_id UNIQUEIDENTIFIER NOT NULL UNIQUE,
    CONSTRAINT uk_record_version UNIQUE (record_id, version),
    CONSTRAINT fk_rr_record FOREIGN KEY (record_id) REFERENCES record (id),
    CONSTRAINT fk_rr_file FOREIGN KEY (file_id) REFERENCES file_store_entry (id)
);
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
    display_name NVARCHAR(255) NOT NULL,
    description NVARCHAR(1000) NULL,
    PRIMARY KEY (entity_type, operation)
);
GO
