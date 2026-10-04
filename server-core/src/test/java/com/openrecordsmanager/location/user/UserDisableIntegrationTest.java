package com.openrecordsmanager.location.user;

import com.openrecordsmanager.api.ResourceIdentifier;
import com.openrecordsmanager.api.builtin.BuiltinConfigs;
import com.openrecordsmanager.api.builtin.BuiltinLocationTypeIds;
import com.openrecordsmanager.api.builtin.BuiltinPlugin;
import com.openrecordsmanager.audit.AuditTestSupport;
import com.openrecordsmanager.auth.JwtSessionService;
import com.openrecordsmanager.auth.dto.SessionMode;
import com.openrecordsmanager.auth.dto.TokenPair;
import com.openrecordsmanager.database.DataRepository;
import com.openrecordsmanager.database.SqliteTestSupport;
import com.openrecordsmanager.rest.exception.ResourceInUseException;
import com.openrecordsmanager.location.LocationService;
import com.openrecordsmanager.location.dto.LocationResponse;
import com.openrecordsmanager.location.dto.NewLocationRequest;
import com.openrecordsmanager.location.dto.UpdateLocationRequest;
import io.jsonwebtoken.Claims;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.security.authentication.DisabledException;
import org.springframework.test.context.DynamicPropertyRegistry;
import org.springframework.test.context.DynamicPropertySource;

import java.util.Map;
import java.util.UUID;

import static org.junit.jupiter.api.Assertions.*;

@SpringBootTest
class UserDisableIntegrationTest {

    @DynamicPropertySource
    static void properties(DynamicPropertyRegistry registry) {
        SqliteTestSupport.registerPrimaryMemoryDatabase(registry, UserDisableIntegrationTest.class);
        registry.add(BuiltinConfigs.PLUGINS_SKIP_SYNC.key(), () -> "true");
        registry.add(BuiltinConfigs.COOKIE_SECURE.key(), () -> "false");
    }

    @Autowired
    private LocationService locationService;

    @Autowired
    private DataRepository repository;

    @Autowired
    private JwtSessionService jwtSessionService;

    private User admin;

    @BeforeEach
    void setUp() {
        this.admin = this.repository.userRepo.findByName("admin").orElseThrow();
    }

    @Test
    void disableUserRevokesTokens() {
        String name = "disable_user_" + UUID.randomUUID().toString().substring(0, 8);

        LocationResponse created = AuditTestSupport.withAudit(this.admin, () -> this.locationService.create(
                new NewLocationRequest(
                        new ResourceIdentifier(BuiltinPlugin.BUILTIN_PLUGIN_NAME, BuiltinLocationTypeIds.USER),
                        null,
                        name,
                        Map.of()
                )
        ));
        assertTrue(created.enabled());

        User user = this.repository.userRepo.findById(created.id()).orElseThrow();
        TokenPair userPair = this.jwtSessionService.issueTokenPair(user, SessionMode.NORMAL);

        LocationResponse disabled = AuditTestSupport.withAudit(this.admin, () -> this.locationService.update(
                this.admin,
                created.id(),
                new UpdateLocationRequest(null, false, null, null)
        ));
        assertFalse(disabled.enabled());

        Claims claims = this.jwtSessionService.verifyAccessToken(userPair.accessToken());
        assertThrows(DisabledException.class, () -> this.jwtSessionService.resolveUser(claims));

        LocationResponse reenabled = AuditTestSupport.withAudit(this.admin, () -> this.locationService.update(
                this.admin,
                created.id(),
                new UpdateLocationRequest(null, true, null, null)
        ));
        assertTrue(reenabled.enabled());
    }

    @Test
    void cannotDisableOwnAccount() {
        assertThrows(
                ResourceInUseException.class,
                () -> AuditTestSupport.withAudit(this.admin, () -> this.locationService.update(
                        this.admin,
                        this.admin.getId(),
                        new UpdateLocationRequest(null, false, null, null)
                ))
        );
    }

}
