-- Initial schema matching current JPA entities (SQL Server)
IF NOT EXISTS (SELECT * FROM sys.tables WHERE name = 'system_configurations')
BEGIN
    CREATE TABLE system_configurations (
        config_key NVARCHAR(255) NOT NULL PRIMARY KEY,
        config_value NVARCHAR(1000)
    );
END
GO

CREATE TABLE auth_provider (
    id UNIQUEIDENTIFIER NOT NULL PRIMARY KEY,
    name NVARCHAR(255) NOT NULL,
    provider_type NVARCHAR(255) NOT NULL,
    settings NVARCHAR(MAX) NOT NULL
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
    default_value NVARCHAR(MAX) NULL,
    user_hidden BIT NOT NULL,
    CONSTRAINT fk_object_property_list_type FOREIGN KEY (list_type_id) REFERENCES list_type (id)
);
GO

CREATE TABLE list_element (
    id NVARCHAR(255) NOT NULL PRIMARY KEY,
    parent_id NVARCHAR(255) NOT NULL,
    name NVARCHAR(255) NOT NULL,
    description NVARCHAR(255) NOT NULL,
    element_index INT NOT NULL,
    active_to DATETIME2 NULL,
    CONSTRAINT fk_list_element_parent FOREIGN KEY (parent_id) REFERENCES list_type (id)
);
GO

CREATE TABLE list_element_alias (
    list_element_id NVARCHAR(255) NOT NULL,
    aliases NVARCHAR(255) NULL,
    CONSTRAINT fk_list_element_alias_element FOREIGN KEY (list_element_id) REFERENCES list_element (id)
);
GO

CREATE TABLE record_type (
    id NVARCHAR(255) NOT NULL PRIMARY KEY,
    name NVARCHAR(255) NOT NULL,
    description NVARCHAR(255) NOT NULL,
    security_filter NVARCHAR(255) NULL,
    security_filter_usage TINYINT NOT NULL CHECK (security_filter_usage BETWEEN 0 AND 2),
    content_types NVARCHAR(MAX) NULL
);
GO

CREATE TABLE record_type_property (
    record_type NVARCHAR(255) NOT NULL,
    property_id NVARCHAR(255) NOT NULL,
    default_value NVARCHAR(MAX) NULL,
    CONSTRAINT fk_rtp_record_type FOREIGN KEY (record_type) REFERENCES record_type (id),
    CONSTRAINT fk_rtp_property FOREIGN KEY (property_id) REFERENCES object_property (id)
);
GO

CREATE TABLE user_details (
    id UNIQUEIDENTIFIER NOT NULL PRIMARY KEY,
    username NVARCHAR(255) NOT NULL UNIQUE,
    auth_provider_id UNIQUEIDENTIFIER NULL,
    CONSTRAINT fk_user_auth_provider FOREIGN KEY (auth_provider_id) REFERENCES auth_provider (id)
);
GO

CREATE TABLE user_property_value (
    id UNIQUEIDENTIFIER NOT NULL PRIMARY KEY,
    user_id UNIQUEIDENTIFIER NOT NULL,
    property_id NVARCHAR(255) NOT NULL,
    property_value NVARCHAR(MAX) NULL,
    CONSTRAINT fk_upv_user FOREIGN KEY (user_id) REFERENCES user_details (id),
    CONSTRAINT fk_upv_property FOREIGN KEY (property_id) REFERENCES object_property (id)
);
GO

CREATE TABLE auth_token (
    token_value NVARCHAR(255) NOT NULL PRIMARY KEY,
    user_id UNIQUEIDENTIFIER NOT NULL,
    expiry_date DATETIME2 NOT NULL,
    CONSTRAINT fk_auth_token_user FOREIGN KEY (user_id) REFERENCES user_details (id)
);
GO

CREATE TABLE file_store (
    id UNIQUEIDENTIFIER NOT NULL PRIMARY KEY,
    type NVARCHAR(255) NOT NULL,
    properties NVARCHAR(MAX) NOT NULL
);
GO

CREATE TABLE file_store_middleware (
    id UNIQUEIDENTIFIER NOT NULL PRIMARY KEY,
    type NVARCHAR(255) NOT NULL,
    properties NVARCHAR(MAX) NOT NULL
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

CREATE TABLE plugin (
    name NVARCHAR(255) NOT NULL PRIMARY KEY,
    version NVARCHAR(255) NOT NULL,
    file_id UNIQUEIDENTIFIER NOT NULL UNIQUE,
    CONSTRAINT fk_plugin_file FOREIGN KEY (file_id) REFERENCES file_store_entry (id)
);
GO

CREATE TABLE record (
    id UNIQUEIDENTIFIER NOT NULL PRIMARY KEY,
    title NVARCHAR(255) NOT NULL,
    type_id NVARCHAR(255) NOT NULL,
    CONSTRAINT fk_record_type FOREIGN KEY (type_id) REFERENCES record_type (id)
);
GO

CREATE TABLE record_property_value (
    id UNIQUEIDENTIFIER NOT NULL PRIMARY KEY,
    record_id UNIQUEIDENTIFIER NOT NULL,
    property_id NVARCHAR(255) NOT NULL,
    property_value NVARCHAR(MAX) NULL,
    CONSTRAINT fk_rpv_record FOREIGN KEY (record_id) REFERENCES record (id),
    CONSTRAINT fk_rpv_property FOREIGN KEY (property_id) REFERENCES object_property (id)
);
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
