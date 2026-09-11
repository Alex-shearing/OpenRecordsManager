package com.openrecordsmanager.plugin.filestore_mssql_filestream;

import com.openrecordsmanager.api.filestore.FileStoreType;
import com.openrecordsmanager.api.schema.SchemaField;
import com.openrecordsmanager.api.schema.SchemaFieldFormat;
import org.jspecify.annotations.Nullable;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.io.FilterInputStream;
import java.io.IOException;
import java.io.InputStream;
import java.sql.*;
import java.util.UUID;

/**
 * Stores files in a Microsoft SQL Server FILESTREAM column via JDBC/TDS streaming.
 *
 * <p>Requires the {@code dbo.orm_filestream_store} table from {@code schema.sql}.
 */
public class MssqlFilestreamFileStoreType
        extends FileStoreType<MssqlFilestreamFileStoreType.MssqlFilestreamFileStoreSettings> {

    private static final Logger LOGGER = LoggerFactory.getLogger(MssqlFilestreamFileStoreType.class);

    private static final String INSERT_SQL =
            "INSERT INTO dbo.filestream_store (id, file_data, file_extension) VALUES (?, ?, ?)";
    private static final String SELECT_SQL =
            "SELECT file_data FROM dbo.filestream_store WHERE id = ?";

    public MssqlFilestreamFileStoreType() {
        super(MssqlFilestreamFileStoreSettings.class);
    }

    @Override
    public String save(
            MssqlFilestreamFileStoreSettings settings,
            InputStream data,
            @Nullable String extension
    ) throws IOException {
        UUID id = UUID.randomUUID();
        String normalizedExtension = normalizeExtension(extension);

        try (Connection connection = openConnection(settings);
             PreparedStatement statement = connection.prepareStatement(INSERT_SQL)) {
            statement.setObject(1, id);
            statement.setBinaryStream(2, data);
            if (normalizedExtension == null) {
                statement.setNull(3, Types.NVARCHAR);
            } else {
                statement.setNString(3, normalizedExtension);
            }
            statement.executeUpdate();
            LOGGER.debug("Saved FILESTREAM object id={}", id);
            return id.toString();
        } catch (SQLException e) {
            throw new IOException("Failed to save file to SQL Server FILESTREAM store", e);
        }
    }

    @Override
    public InputStream retrieve(MssqlFilestreamFileStoreSettings settings, String data) throws IOException {
        UUID id;
        try {
            id = UUID.fromString(data);
        } catch (IllegalArgumentException e) {
            throw new IOException("Invalid FILESTREAM object id: " + data, e);
        }

        Connection connection = null;
        PreparedStatement statement = null;
        ResultSet resultSet = null;
        try {
            connection = openConnection(settings);
            statement = connection.prepareStatement(SELECT_SQL);
            statement.setObject(1, id);
            resultSet = statement.executeQuery();
            if (!resultSet.next()) {
                throw new IOException("FILESTREAM object not found: id=" + data);
            }

            InputStream stream = resultSet.getBinaryStream(1);
            if (stream == null) {
                throw new IOException("FILESTREAM object has null data: id=" + data);
            }

            return new ClosingResultSetInputStream(stream, resultSet, statement, connection);
        } catch (SQLException | IOException e) {
            closeQuietly(resultSet);
            closeQuietly(statement);
            closeQuietly(connection);
            if (e instanceof IOException ioException) {
                throw ioException;
            }
            throw new IOException("Failed to retrieve file from SQL Server FILESTREAM store", e);
        }
    }

    private static Connection openConnection(MssqlFilestreamFileStoreSettings settings) throws SQLException {
        try {
            // Ensure the driver is visible when this plugin is loaded from an isolated classloader.
            Class.forName("com.microsoft.sqlserver.jdbc.SQLServerDriver");
        } catch (ClassNotFoundException e) {
            throw new SQLException(
                    "Microsoft SQL Server JDBC driver not found. Ensure mssql-jdbc is on the server classpath.",
                    e
            );
        }
        return DriverManager.getConnection(settings.jdbcUrl(), settings.username(), settings.password());
    }

    private static @Nullable String normalizeExtension(@Nullable String extension) {
        if (extension == null) {
            return null;
        }
        String trimmed = extension.trim();
        if (trimmed.isEmpty()) {
            return null;
        }
        if (trimmed.startsWith(".")) {
            trimmed = trimmed.substring(1);
        }
        return trimmed.isEmpty() ? null : trimmed;
    }

    private static void closeQuietly(@Nullable AutoCloseable closeable) {
        if (closeable == null) {
            return;
        }
        try {
            closeable.close();
        } catch (Exception ignored) {
            // best-effort cleanup after a prior failure
        }
    }

    /**
     * Ensures JDBC resources stay open for the life of the blob stream and are closed with it.
     */
    private static final class ClosingResultSetInputStream extends FilterInputStream {
        private final ResultSet resultSet;
        private final PreparedStatement statement;
        private final Connection connection;
        private boolean closed;

        private ClosingResultSetInputStream(
                InputStream stream,
                ResultSet resultSet,
                PreparedStatement statement,
                Connection connection
        ) {
            super(stream);
            this.resultSet = resultSet;
            this.statement = statement;
            this.connection = connection;
        }

        @Override
        public void close() throws IOException {
            if (this.closed) {
                return;
            }
            this.closed = true;
            try {
                super.close();
            } finally {
                closeQuietly(this.resultSet);
                closeQuietly(this.statement);
                closeQuietly(this.connection);
            }
        }
    }

    public record MssqlFilestreamFileStoreSettings(
            @SchemaField(title = "JDBC URL", minLength = 1) String jdbcUrl,
            @SchemaField(title = "Username", minLength = 1) String username,
            @SchemaField(title = "Password", format = SchemaFieldFormat.PASSWORD, minLength = 1) String password
    ) {
    }
}
