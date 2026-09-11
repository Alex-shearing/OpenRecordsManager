# filestore-mssql-filestream

Stores file blobs in a Microsoft SQL Server **FILESTREAM** column using JDBC/TDS streaming.

## Runtime type id

`filestore_mssql_filestream:filestream`

## Settings

| Field      | Description                                                       |
|------------|-------------------------------------------------------------------|
| `jdbcUrl`  | SQL Server JDBC URL (must point at a FILESTREAM-enabled database) |
| `username` | Database user                                                     |
| `password` | Database password                                                 |

## Schema

Run [`schema.sql`](src/main/resources/schema.sql) (or equivalent) before creating a file store instance. The plugin does
not create the table or FILESTREAM filegroup.

Columns:

- `id` — `UNIQUEIDENTIFIER ROWGUIDCOL`; returned as the opaque store key
- `file_data` — `VARBINARY(MAX) FILESTREAM`
- `file_extension` — extension without a leading dot (for future Full-Text Search `TYPE COLUMN`)
- `created_at` — UTC insert time
