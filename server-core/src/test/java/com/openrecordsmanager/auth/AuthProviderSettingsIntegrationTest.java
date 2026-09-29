package com.openrecordsmanager.auth;

import com.openrecordsmanager.api.ComponentReference;
import com.openrecordsmanager.api.ResourceIdentifier;
import com.openrecordsmanager.api.builtin.BuiltinConfigs;
import com.openrecordsmanager.api.errors.ApiException;
import com.openrecordsmanager.api.types.ComponentTypes;
import com.openrecordsmanager.audit.AuditTestSupport;
import com.openrecordsmanager.auth.dto.AuthProviderResponse;
import com.openrecordsmanager.auth.dto.AuthProviderTypeResponse;
import com.openrecordsmanager.auth.dto.UpdateAuthProviderRequest;
import com.openrecordsmanager.database.DataRepository;
import com.openrecordsmanager.database.SqliteTestSupport;
import com.openrecordsmanager.user.User;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.test.context.DynamicPropertyRegistry;
import org.springframework.test.context.DynamicPropertySource;

import java.util.Arrays;
import java.util.Map;
import java.util.UUID;

import static org.junit.jupiter.api.Assertions.*;

@SpringBootTest
class AuthProviderSettingsIntegrationTest {

    @DynamicPropertySource
    static void properties(DynamicPropertyRegistry registry) {
        SqliteTestSupport.registerPrimaryMemoryDatabase(registry, AuthProviderSettingsIntegrationTest.class);
        registry.add(BuiltinConfigs.PLUGINS_SKIP_SYNC.key(), () -> "true");
        registry.add(BuiltinConfigs.COOKIE_SECURE.key(), () -> "false");
    }

    @Autowired
    private AuthService authService;

    @Autowired
    private DataRepository repository;

    private User admin;

    @BeforeEach
    void setUp() {
        this.admin = this.repository.userRepo.findByUsername("admin").orElseThrow();
    }

    @Test
    void listProviderTypesIncludesLocalAndOidcWithSchemas() {
        AuthProviderTypeResponse[] types = this.authService.listProviderTypes();

        assertTrue(Arrays.stream(types).anyMatch(t -> "auth_local:local_auth".equals(t.type().id().toString())));
        assertTrue(Arrays.stream(types).anyMatch(t -> "auth_oidc:oidc_auth".equals(t.type().id().toString())));

        AuthProviderTypeResponse local = Arrays.stream(types)
                .filter(t -> "auth_local:local_auth".equals(t.type().id().toString()))
                .findFirst()
                .orElseThrow();
        assertNull(local.settingsSchema().properties());

        AuthProviderTypeResponse oidc = Arrays.stream(types)
                .filter(t -> "auth_oidc:oidc_auth".equals(t.type().id().toString()))
                .findFirst()
                .orElseThrow();
        assertEquals(Boolean.TRUE, oidc.settingsSchema().properties().get("secret").writeOnly());
    }

    @Test
    void createOidcProviderRejectsInvalidSettings() {
        assertThrows(
                ApiException.class,
                () -> AuditTestSupport.withAudit(this.admin, () -> this.authService.createProvider(
                        "Broken OIDC",
                        ComponentReference.of(
                                ComponentTypes.REDIRECT_AUTH_PROVIDER,
                                ResourceIdentifier.valueOf("auth_oidc:oidc_auth")
                        ),
                        Map.of("clientId", "x")
                ))
        );
    }

    @Test
    void createAndUpdateOidcProviderRedactsSecretAndPreservesOnBlankUpdate() {
        AuthProviderResponse created = AuditTestSupport.withAudit(this.admin, () -> this.authService.createProvider(
                "Test OIDC",
                ComponentReference.of(
                        ComponentTypes.REDIRECT_AUTH_PROVIDER,
                        ResourceIdentifier.valueOf("auth_oidc:oidc_auth")
                ),
                Map.of(
                        "clientId", "orm-client",
                        "secret", "super-secret-value",
                        "uri", "http://localhost:9000/application/o/orm/",
                        "scope", "openid profile",
                        "usernameClaim", "preferred_username"
                )
        ));

        assertTrue(created.enabled());
        assertEquals("orm-client", created.settings().get("clientId"));
        assertFalse(created.settings().containsKey("secret"));
        assertEquals("http://localhost:9000/application/o/orm/", created.settings().get("uri"));
        assertFalse(created.toString().contains("super-secret-value"));

        UUID id = created.id();

        AuthProviderResponse updated = AuditTestSupport.withAudit(this.admin, () -> this.authService.updateProvider(
                id,
                new UpdateAuthProviderRequest(
                        null,
                        null,
                        Map.of(
                                "clientId", "orm-client-updated",
                                "secret", "",
                                "uri", "http://localhost:9000/application/o/orm/",
                                "scope", "openid profile email",
                                "usernameClaim", "preferred_username"
                        )
                )
        ));

        assertEquals("orm-client-updated", updated.settings().get("clientId"));
        assertFalse(updated.settings().containsKey("secret"));

        AuthProviderResponse loaded = this.authService.getProvider(id);
        assertEquals("orm-client-updated", loaded.settings().get("clientId"));
        assertFalse(loaded.settings().containsKey("secret"));
    }

}
