package com.openrecordsmanager.database.schema;

import com.openrecordsmanager.api.ComponentReference;
import com.openrecordsmanager.api.ResourceIdentifier;
import com.openrecordsmanager.api.types.ComponentTypes;
import com.openrecordsmanager.auth.AuthService;
import com.openrecordsmanager.auth.dto.AuthProviderListResponse;
import com.openrecordsmanager.auth.entity.AuthProvider;
import com.openrecordsmanager.database.DataRepository;
import com.openrecordsmanager.plugin.registry.ComponentCatalog;
import com.openrecordsmanager.user.User;
import com.openrecordsmanager.user.UserService;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.boot.context.event.ApplicationReadyEvent;
import org.springframework.context.event.EventListener;
import org.springframework.stereotype.Component;
import org.springframework.transaction.annotation.Transactional;

import java.util.Map;

/**
 * Seeds bootstrap auth data after Flyway + JPA are fully started.
 * <p>
 * {@link SchemaMigrationService#evaluate()} runs inside Flyway initialization, before
 * {@code EntityManagerFactory} exists, so repositories cannot be used there.
 */
@Component
public class InitialDatabaseSeeder {
    private static final Logger LOGGER = LoggerFactory.getLogger(InitialDatabaseSeeder.class);

    private static final ResourceIdentifier LOCAL_AUTH_TYPE = ResourceIdentifier.valueOf("auth_local:local_auth");
    private static final ResourceIdentifier RESET_PASSWORD_ACTION = ResourceIdentifier.valueOf("auth_local:reset_password");

    private final SchemaMigrationState state;
    private final AuthService authService;
    private final UserService userService;
    private final DataRepository repository;
    private final ComponentCatalog catalog;

    public InitialDatabaseSeeder(
            SchemaMigrationState state,
            AuthService authService,
            UserService userService,
            DataRepository repository,
            ComponentCatalog catalog
    ) {
        this.state = state;
        this.authService = authService;
        this.userService = userService;
        this.repository = repository;
        this.catalog = catalog;
    }

    @EventListener(ApplicationReadyEvent.class)
    @Transactional
    public void seedIfNeeded() {
        if (!this.state.consumeInitialSeedPending()) {
            return;
        }

        if (this.catalog.getRegistry(ComponentTypes.INPUT_AUTH_PROVIDER).get(LOCAL_AUTH_TYPE).isEmpty()) {
            LOGGER.warn("Skipping initial database seed: {} is not registered", LOCAL_AUTH_TYPE);
            return;
        }

        LOGGER.info("Seeding default local auth provider and admin user");

        AuthProviderListResponse created = this.authService.createProvider(
                "Local Authentication",
                ComponentReference.of(ComponentTypes.INPUT_AUTH_PROVIDER, LOCAL_AUTH_TYPE),
                Map.of()
        );

        AuthProvider provider = this.repository.authProviderRepo.findById(created.id())
                .orElseThrow(() -> new IllegalStateException("Failed to load seeded auth provider"));

        User admin = new User("admin", provider);
        this.repository.userRepo.saveAndFlush(admin);

        this.userService.executeAction(
                admin,
                admin.getId(),
                RESET_PASSWORD_ACTION,
                Map.of("newPassword", "admin")
        );

        LOGGER.info("Seeded local auth provider {} and admin user {}", provider.getId(), admin.getId());
    }
}
