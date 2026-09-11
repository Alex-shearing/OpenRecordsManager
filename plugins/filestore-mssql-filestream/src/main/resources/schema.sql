-- Microsoft SQL Server FILESTREAM table for the filestore_mssql_filestream plugin.
-- Prerequisites:
--   1. FILESTREAM enabled at the SQL Server instance level
--   2. A FILESTREAM filegroup on the target database
--
-- Configure the file store instance JDBC URL to this database.
-- Table name is fixed as dbo.orm_filestream_store (used by the plugin SQL).
-- file_extension stores the extension without a leading dot (pdf, jar, docx) for future
-- Full-Text Search TYPE COLUMN use.

CREATE TABLE dbo.filestream_store (
    id UNIQUEIDENTIFIER ROWGUIDCOL NOT NULL UNIQUE DEFAULT NEWSEQUENTIALID(),
    file_data VARBINARY(MAX) FILESTREAM NOT NULL,
    file_extension NVARCHAR(32) NULL,
    created_at DATETIME2 NOT NULL DEFAULT SYSUTCDATETIME()
);

-- Future content search (not created by this plugin):
-- CREATE FULLTEXT CATALOG filestream_catalog AS DEFAULT;
-- CREATE FULLTEXT INDEX ON dbo.filestream_store (file_data TYPE COLUMN file_extension)
--     KEY INDEX <unique_index_on_id>
--     ON orm_filestream_catalog;
