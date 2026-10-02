package com.openrecordsmanager.location.user;

import com.openrecordsmanager.api.builtin.BuiltinConfigs;
import com.openrecordsmanager.audit.AuditTestSupport;
import com.openrecordsmanager.auth.JwtSessionService;
import com.openrecordsmanager.auth.dto.SessionMode;
import com.openrecordsmanager.auth.dto.TokenPair;
import com.openrecordsmanager.database.DataRepository;
import com.openrecordsmanager.database.SqliteTestSupport;
import com.openrecordsmanager.rest.exception.ResourceInUseException;
import com.openrecordsmanager.location.user.dto.NewUserRequest;
import com.openrecordsmanager.location.user.dto.UpdateUserRequest;
import com.openrecordsmanager.location.user.dto.UserResponse;
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
    private UserService userService;

    @Autowired
    private DataRepository repository;

    @Autowired
    private JwtSessionService jwtSessionService;

    private User admin;

    @BeforeEach
    void setUp() {
        this.admin = this.repository.userRepo.findByUsername("admin").orElseThrow();
    }

    @Test
    void disableUserRevokesTokens() {
        String username = "disable_user_" + UUID.randomUUID().toString().substring(0, 8);

        UserResponse created = AuditTestSupport.withAudit(this.admin, () -> this.userService.create(
                new NewUserRequest(username, null, Map.of())
        ));
        assertTrue(created.enabled());

        User user = this.repository.userRepo.findById(created.id()).orElseThrow();
        TokenPair userPair = this.jwtSessionService.issueTokenPair(user, SessionMode.NORMAL);

        UserResponse disabled = AuditTestSupport.withAudit(this.admin, () -> this.userService.update(
                this.admin,
                created.id(),
                new UpdateUserRequest(null, null, false, null)
        ));
        assertFalse(disabled.enabled());

        Claims claims = this.jwtSessionService.verifyAccessToken(userPair.accessToken());
        assertThrows(DisabledException.class, () -> this.jwtSessionService.resolveUser(claims));

        UserResponse reenabled = AuditTestSupport.withAudit(this.admin, () -> this.userService.update(
                this.admin,
                created.id(),
                new UpdateUserRequest(null, null, true, null)
        ));
        assertTrue(reenabled.enabled());
    }

    @Test
    void cannotDisableOwnAccount() {
        assertThrows(
                ResourceInUseException.class,
                () -> AuditTestSupport.withAudit(this.admin, () -> this.userService.update(
                        this.admin,
                        this.admin.getId(),
                        new UpdateUserRequest(null, null, false, null)
                ))
        );
    }

}
