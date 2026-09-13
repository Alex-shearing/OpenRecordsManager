# filestore-mssql-filestream

Stores file blobs in a Microsoft SQL Server **FILESTREAM** column using JDBC/TDS streaming.

## Runtime type id

`filestore_mssql_filestream:filestream`

## Settings

| Field      | Description                                                                               |
| ---------- | ----------------------------------------------------------------------------------------- |
| `jdbcUrl`  | SQL Server JDBC URL (must point at a FILESTREAM-enabled **user** database — not `master`) |
| `username` | Database user                                                                             |
| `password` | Database password                                                                         |

Example JDBC URL:

```text
jdbc:sqlserver://localhost:1433;databaseName=orm_files;encrypt=true;trustServerCertificate=true
```

## Prerequisites

1. FILESTREAM enabled at the SQL Server **instance** level
2. A **FILESTREAM filegroup** (and file) on the target database
3. JDBC URL must use that user database (`databaseName=...`), not `master` / `tempdb` / etc.

On create/update, the plugin connects and creates `dbo.filestream_store` if it is missing. It does **not** create the FILESTREAM filegroup for you.

## Schema columns

- `id` — `UNIQUEIDENTIFIER ROWGUIDCOL`; returned as the opaque store key
- `file_data` — `VARBINARY(MAX) FILESTREAM`
- `file_extension` — extension without a leading dot (for future Full-Text Search `TYPE COLUMN`)
- `created_at` — UTC insert time
