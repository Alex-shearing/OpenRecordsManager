package com.openrecordsmanager.database;

import com.jayway.jsonpath.JsonPath;
import com.openrecordsmanager.api.ResourceIdentifier;
import com.openrecordsmanager.api.builtin.BuiltinConfigs;
import com.openrecordsmanager.api.builtin.BuiltinPlugin;
import com.openrecordsmanager.api.builtin.BuiltinProperties;
import com.openrecordsmanager.api.builtin.BuiltinPropertyIds;
import com.openrecordsmanager.database.schema.SchemaMigrationState;
import org.flywaydb.core.Flyway;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.boot.webmvc.test.autoconfigure.AutoConfigureMockMvc;
import org.springframework.http.MediaType;
import org.springframework.test.annotation.DirtiesContext;
import org.springframework.test.context.DynamicPropertyRegistry;
import org.springframework.test.context.DynamicPropertySource;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.test.web.servlet.MvcResult;
import org.springframework.test.web.servlet.ResultMatcher;

import java.io.IOException;
import java.nio.ByteBuffer;
import java.nio.file.Files;
import java.nio.file.Path;
import java.sql.Connection;
import java.sql.DriverManager;
import java.sql.PreparedStatement;
import java.sql.Timestamp;
import java.time.Instant;
import java.util.UUID;

import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertNotEquals;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

@SpringBootTest
@AutoConfigureMockMvc
@DirtiesContext(classMode = DirtiesContext.ClassMode.AFTER_CLASS)
class PrimaryOfflineIntegrationTest {

    private static final Path READ_DB = Path.of("build/test-primary-offline-read.db");
    private static final UUID PROVIDER_ID = UUID.fromString("11111111-1111-1111-1111-111111111111");
    private static final UUID USER_ID = UUID.fromString("22222222-2222-2222-2222-222222222222");
    // bcrypt hash for "admin"
    private static final String ADMIN_PASSWORD_HASH = "$2a$10$NP32LOP2SW4iI7pMQPpqZOCwJF81s9K/vnIfH6kPQXmKsTD653RWq";

    static {
        try {
            Files.deleteIfExists(READ_DB);
            Files.createDirectories(READ_DB.getParent());
        } catch (IOException e) {
            throw new RuntimeException(e);
        }
        String jdbcUrl = "jdbc:sqlite:" + READ_DB + "?busy_timeout=5000";
        Flyway.configure()
                .dataSource(jdbcUrl, "", "")
                .locations("classpath:db/migration/sqlite")
                .load()
                .migrate();
        seedAuthData(jdbcUrl);
    }

    private static void seedAuthData(String jdbcUrl) {
        try (Connection connection = DriverManager.getConnection(jdbcUrl)) {
            Timestamp now = Timestamp.from(Instant.now());

            // /api/location/me serializes User wire properties via ObjectPropertyLookup; primary seeding
            // is skipped when the primary DB is offline, so builtins must exist on the read replica.
            try (PreparedStatement property = connection.prepareStatement(
                    "INSERT INTO object_property (id, type, user_hidden, date_created, date_modified) VALUES (?, ?, ?, ?, ?)"
            )) {
                BuiltinProperties.BUILTIN_PROPERTIES.forEach((id, template) -> {
                    try {
                        seedObjectProperty(
                                property,
                                now,
                                new ResourceIdentifier(BuiltinPlugin.BUILTIN_PLUGIN_NAME, id).toString(),
                                template.type().getName(),
                                template.userHidden()
                        );
                    } catch (Exception e) {
                        throw new RuntimeException("Failed to seed builtin property " + id, e);
                    }
                });
                seedObjectProperty(property, now, "auth_local:password_hash", "string", true);
            }

            try (PreparedStatement provider = connection.prepareStatement(
                    "INSERT INTO auth_provider (id, name, provider_type, settings, enabled, date_created, date_modified) VALUES (?, ?, ?, ?, ?, ?, ?)"
            )) {
                provider.setBytes(1, uuidBytes(PROVIDER_ID));
                provider.setString(2, "Local Authentication");
                provider.setString(3, "input_auth_provider/auth_local:local_auth");
                provider.setString(4, "{}");
                provider.setBoolean(5, true);
                provider.setTimestamp(6, now);
                provider.setTimestamp(7, now);
                provider.executeUpdate();
            }

            try (PreparedStatement locationType = connection.prepareStatement(
                    """
                            INSERT INTO location_type
                            (id, kind, date_created, date_modified)
                            VALUES (?, ?, ?, ?)
                            """
            )) {
                locationType.setString(1, "builtin:user");
                locationType.setString(2, "USER");
                locationType.setTimestamp(3, now);
                locationType.setTimestamp(4, now);
                locationType.executeUpdate();
            }

            try (PreparedStatement typeProperty = connection.prepareStatement(
                    "INSERT INTO location_type_property (location_type, property_id, default_value) VALUES (?, ?, NULL)"
            )) {
                for (String propertyId : new String[]{
                        BuiltinPropertyIds.NAME,
                        BuiltinPropertyIds.NOTES,
                        BuiltinPropertyIds.DATE_CREATED,
                        BuiltinPropertyIds.DATE_MODIFIED,
                        BuiltinPropertyIds.GIVEN_NAME,
                        BuiltinPropertyIds.SURNAME,
                        BuiltinPropertyIds.HONORIFIC,
                        BuiltinPropertyIds.EMAIL
                }) {
                    typeProperty.setString(1, "builtin:user");
                    typeProperty.setString(
                            2,
                            new ResourceIdentifier(BuiltinPlugin.BUILTIN_PLUGIN_NAME, propertyId).toString()
                    );
                    typeProperty.executeUpdate();
                }
            }

            try (PreparedStatement location = connection.prepareStatement(
                    """
                            INSERT INTO location
                            (id, name, notes, date_created, date_modified, location_kind, type_id)
                            VALUES (?, ?, NULL, ?, ?, ?, ?)
                            """
            )) {
                location.setBytes(1, uuidBytes(USER_ID));
                location.setString(2, "admin");
                location.setTimestamp(3, now);
                location.setTimestamp(4, now);
                location.setString(5, "USER");
                location.setString(6, "builtin:user");
                location.executeUpdate();
            }

            try (PreparedStatement user = connection.prepareStatement(
                    """
                            INSERT INTO user_details
                            (id, auth_provider_id, enabled, session_epoch)
                            VALUES (?, ?, ?, ?)
                            """
            )) {
                user.setBytes(1, uuidBytes(USER_ID));
                user.setBytes(2, uuidBytes(PROVIDER_ID));
                user.setBoolean(3, true);
                user.setInt(4, 0);
                user.executeUpdate();
            }

            try (PreparedStatement value = connection.prepareStatement(
                    "INSERT INTO location_property_value (location_id, property_id, property_value) VALUES (?, ?, ?)"
            )) {
                value.setBytes(1, uuidBytes(USER_ID));
                value.setString(2, "auth_local:password_hash");
                value.setString(3, "\"" + ADMIN_PASSWORD_HASH + "\"");
                value.executeUpdate();
            }
        } catch (Exception e) {
            throw new RuntimeException("Failed to seed primary-offline read database", e);
        }
    }

    private static void seedObjectProperty(
            PreparedStatement property,
            Timestamp now,
            String id,
            String type,
            boolean userHidden
    ) throws Exception {
        property.setString(1, id);
        property.setString(2, type);
        property.setBoolean(3, userHidden);
        property.setTimestamp(4, now);
        property.setTimestamp(5, now);
        property.executeUpdate();
    }

    private static byte[] uuidBytes(UUID uuid) {
        ByteBuffer buffer = ByteBuffer.wrap(new byte[16]);
        buffer.putLong(uuid.getMostSignificantBits());
        buffer.putLong(uuid.getLeastSignificantBits());
        return buffer.array();
    }

    @DynamicPropertySource
    static void primaryOffline(DynamicPropertyRegistry registry) {
        registry.add("orm.test.context", () -> "primary-offline");
        registry.add("server.database.primary.url",
                () -> "jdbc:sqlite:file:/does/not/exist/orm_primary_offline.db");
        registry.add("server.database.primary.driver-class-name", () -> "org.sqlite.JDBC");
        registry.add("server.database.read-only.url",
                () -> "jdbc:sqlite:" + READ_DB + "?open_mode=1");
        registry.add("server.database.read-only.driver-class-name", () -> "org.sqlite.JDBC");
        registry.add(BuiltinConfigs.PLUGINS_SKIP_SYNC.key(), () -> "true");
        registry.add(BuiltinConfigs.DATABASE_PROBE_INTERVAL_MS.key(), () -> "60000");
        registry.add(BuiltinConfigs.AUDIT_SPOOL_DRAIN_INTERVAL_SECONDS.key(), () -> "60000");
        registry.add(BuiltinConfigs.COOKIE_SECURE.key(), () -> "false");
    }

    @Autowired
    private DatabaseWritableProbe probe;

    @Autowired
    private SchemaMigrationState schemaMigrationState;

    @Autowired
    private MockMvc mockMvc;

    @Test
    void applicationStartsWithOfflinePrimaryAndServesReadRequests() throws Exception {
        assertFalse(this.probe.isWritable());
        assertFalse(this.schemaMigrationState.isUpgradeRequired());

        this.mockMvc.perform(get("/api/database/status").accept(MediaType.APPLICATION_JSON))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.data.state").value("READY"));

        this.mockMvc.perform(get("/api/auth/available_providers").accept(MediaType.APPLICATION_JSON))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.data.length()").value(1));
    }

    @Test
    void loginAndRefreshWorkWhenPrimaryIsOffline() throws Exception {
        assertFalse(this.probe.isWritable());

        MvcResult loginResult = this.mockMvc.perform(
                        post("/api/auth/login/" + PROVIDER_ID)
                                .contentType(MediaType.APPLICATION_JSON)
                                .content("""
                                        {
                                          "username": "admin",
                                          "password": "admin"
                                        }
                                        """)
                                .accept(MediaType.APPLICATION_JSON)
                )
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.data.accessToken").exists())
                .andExpect(jsonPath("$.data.refreshToken").exists())
                .andExpect(jsonPath("$.data.sessionMode").value("DEGRADED_READ_ONLY"))
                .andReturn();

        String body = loginResult.getResponse().getContentAsString();
        String accessToken = JsonPath.read(body, "$.data.accessToken");
        String refreshToken = JsonPath.read(body, "$.data.refreshToken");

        this.mockMvc.perform(
                        get("/api/location/me")
                                .header("Authorization", "Bearer " + accessToken)
                                .accept(MediaType.APPLICATION_JSON)
                )
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.data.name").value("admin"))
                .andExpect(headerSessionModeDegraded());

        MvcResult refreshResult = this.mockMvc.perform(
                        post("/api/auth/refresh")
                                .header("Authorization", "Bearer " + refreshToken)
                                .accept(MediaType.APPLICATION_JSON)
                )
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.data.sessionMode").value("DEGRADED_READ_ONLY"))
                .andReturn();

        String refreshedAccess = JsonPath.read(refreshResult.getResponse().getContentAsString(), "$.data.accessToken");
        assertNotEquals(accessToken, refreshedAccess);

        this.mockMvc.perform(
                        get("/api/location/me")
                                .header("Authorization", "Bearer " + refreshedAccess)
                                .accept(MediaType.APPLICATION_JSON)
                )
                .andExpect(status().isOk());
    }

    private static ResultMatcher headerSessionModeDegraded() {
        return result -> {
            String header = result.getResponse().getHeader("X-ORM-Session-Mode");
            if (!"DEGRADED_READ_ONLY".equals(header)) {
                throw new AssertionError("Expected X-ORM-Session-Mode=DEGRADED_READ_ONLY but was " + header);
            }
        };
    }
}
