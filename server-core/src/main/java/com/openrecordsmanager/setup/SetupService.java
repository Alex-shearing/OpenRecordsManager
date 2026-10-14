package com.openrecordsmanager.setup;

import com.openrecordsmanager.api.ComponentReference;
import com.openrecordsmanager.api.ResourceIdentifier;
import com.openrecordsmanager.api.auth.InputAuthProviderType;
import com.openrecordsmanager.api.builtin.BuiltinLocationTypeIds;
import com.openrecordsmanager.api.builtin.BuiltinPlugin;
import com.openrecordsmanager.api.types.ComponentTypes;
import com.openrecordsmanager.audit.AuditContext;
import com.openrecordsmanager.auth.AuthService;
import com.openrecordsmanager.auth.dto.AuthProviderResponse;
import com.openrecordsmanager.auth.entity.AuthProvider;
import com.openrecordsmanager.database.DataRepository;
import com.openrecordsmanager.database.schema.BuiltinTemplateSeeder;
import com.openrecordsmanager.database.schema.SchemaMigrationReadyEvent;
import com.openrecordsmanager.database.schema.SchemaMigrationState;
import com.openrecordsmanager.location.LocationService;
import com.openrecordsmanager.location.type.LocationType;
import com.openrecordsmanager.location.user.User;
import com.openrecordsmanager.plugin.registry.ComponentCatalog;
import com.openrecordsmanager.rest.exception.ResourceAlreadyExistsException;
import com.openrecordsmanager.rest.exception.ResourceNotFoundException;
import com.openrecordsmanager.setup.dto.CompleteSetupRequest;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.boot.context.event.ApplicationReadyEvent;
import org.springframework.context.event.EventListener;
import org.springframework.core.Ordered;
import org.springframework.core.annotation.Order;
import org.springframework.http.HttpStatus;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.web.server.ResponseStatusException;

import java.util.Map;

@Service
public class SetupService {
    private static final Logger LOGGER = LoggerFactory.getLogger(SetupService.class);

    private static final ResourceIdentifier LOCAL_AUTH_TYPE = ResourceIdentifier.valueOf("auth_local:local_auth");
    private static final ResourceIdentifier RESET_PASSWORD_ACTION = ResourceIdentifier.valueOf("auth_local:reset_password");

    private final DataRepository repository;
    private final ComponentCatalog catalog;
    private final AuthService authService;
    private final LocationService locationService;
    private final BuiltinTemplateSeeder builtinTemplateSeeder;
    private final SchemaMigrationState schemaState;

    public SetupService(
            DataRepository repository,
            ComponentCatalog catalog,
            AuthService authService,
            LocationService locationService,
            BuiltinTemplateSeeder builtinTemplateSeeder,
            SchemaMigrationState schemaState
    ) {
        this.repository = repository;
        this.catalog = catalog;
        this.authService = authService;
        this.locationService = locationService;
        this.builtinTemplateSeeder = builtinTemplateSeeder;
        this.schemaState = schemaState;
    }

    public boolean isSetupRequired() {
        return this.repository.userRepo.count() == 0;
    }

    /**
     * Aligns {@link SchemaMigrationState} with first-run setup after schema evaluation.
     * Runs after JPA is available (Flyway evaluate cannot query users yet).
     */
    @EventListener({ApplicationReadyEvent.class, SchemaMigrationReadyEvent.class})
    @Order(Ordered.HIGHEST_PRECEDENCE)
    public void syncSchemaState() {
        if (this.schemaState.isUpgradeRequired()) {
            return;
        }
        if (this.isSetupRequired()) {
            this.schemaState.markSetupRequired(this.schemaState.getCurrentVersion());
        } else {
            this.schemaState.markReady(this.schemaState.getCurrentVersion());
        }
    }

    @Transactional
    public void complete(CompleteSetupRequest request) {
        if (!this.isSetupRequired()) {
            throw new ResourceAlreadyExistsException("setup", "already_complete");
        }
        if (this.schemaState.isUpgradeRequired()) {
            throw new ResponseStatusException(HttpStatus.SERVICE_UNAVAILABLE, "schema_upgrade_required");
        }

        ComponentReference<InputAuthProviderType<?, ?>> localAuthType =
                ComponentReference.of(ComponentTypes.INPUT_AUTH_PROVIDER, LOCAL_AUTH_TYPE);
        if (this.catalog.get(localAuthType).isEmpty()) {
            throw new ResourceNotFoundException(ComponentTypes.INPUT_AUTH_PROVIDER, LOCAL_AUTH_TYPE);
        }

        AuditContext.disableCapture();
        try {
            this.builtinTemplateSeeder.seedAll();

            LOGGER.info("Completing first-run setup for user {}", request.username());

            AuthProviderResponse created = this.authService.createProvider(
                    "Local Authentication",
                    localAuthType,
                    Map.of()
            );

            AuthProvider provider = this.repository.authProviderRepo.findById(created.id())
                    .orElseThrow(() -> new IllegalStateException("Failed to load created auth provider"));

            LocationType userType = this.repository.locationTypeRepo
                    .findById(new ResourceIdentifier(BuiltinPlugin.BUILTIN_PLUGIN_NAME, BuiltinLocationTypeIds.USER))
                    .orElseThrow(() -> new IllegalStateException("Builtin location type 'user' is not registered"));

            User admin = new User(request.username(), provider, userType);
            this.repository.userRepo.saveAndFlush(admin);

            this.locationService.executeAction(
                    admin,
                    admin.getId(),
                    RESET_PASSWORD_ACTION,
                    Map.of("newPassword", request.password())
            );

            LOGGER.info("First-run setup complete for user {}", admin.getUsername());
            this.syncSchemaState();
        } finally {
            AuditContext.clear();
        }
    }
}
