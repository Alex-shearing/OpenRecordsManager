package com.openrecordsmanager.database;

import org.jspecify.annotations.Nullable;
import org.springframework.jdbc.datasource.AbstractDataSource;

import javax.sql.DataSource;
import java.sql.Connection;
import java.sql.SQLException;

/**
 * Flyway-only datasource: uses primary when reachable, otherwise read replica for schema inspection.
 */
final class FlywayDataSourceSelector extends AbstractDataSource {

    private final DataSource writeDataSource;
    private final DataSource readDataSource;

    FlywayDataSourceSelector(DataSource writeDataSource, DataSource readDataSource) {
        this.writeDataSource = writeDataSource;
        this.readDataSource = readDataSource;
    }

    @Override
    public Connection getConnection() throws SQLException {
        return this.connect(null, null);
    }

    @Override
    public Connection getConnection(String username, String password) throws SQLException {
        return this.connect(username, password);
    }

    private Connection connect(@Nullable String username, @Nullable String password) throws SQLException {
        try {
            return username == null
                    ? this.writeDataSource.getConnection()
                    : this.writeDataSource.getConnection(username, password);
        } catch (Exception writeFailure) {
            try {
                return username == null
                        ? this.readDataSource.getConnection()
                        : this.readDataSource.getConnection(username, password);
            } catch (Exception readFailure) {
                readFailure.addSuppressed(writeFailure);
                if (readFailure instanceof SQLException sql) {
                    throw sql;
                }
                throw new SQLException("Failed to connect to read replica", readFailure);
            }
        }
    }
}
