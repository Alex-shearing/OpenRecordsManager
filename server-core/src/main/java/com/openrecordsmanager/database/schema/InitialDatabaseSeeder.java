package com.openrecordsmanager.database.schema;

import com.openrecordsmanager.api.ComponentReference;
import com.openrecordsmanager.api.ResourceIdentifier;
import com.openrecordsmanager.api.builtin.BuiltinLocationTypeIds;
import com.openrecordsmanager.api.builtin.BuiltinPlugin;
import com.openrecordsmanager.api.template.location.LocationRelationshipTypeTemplate;
import com.openrecordsmanager.api.template.location.LocationTypeTemplate;
import com.openrecordsmanager.api.template.property.ObjectPropertyTemplate;
import com.openrecordsmanager.api.types.ComponentTypes;
import com.openrecordsmanager.audit.AuditContext;
import com.openrecordsmanager.audit.AuditService;
import com.openrecordsmanager.auth.AuthService;
import com.openrecordsmanager.auth.dto.AuthProviderResponse;
import com.openrecordsmanager.auth.entity.AuthProvider;
import com.openrecordsmanager.database.DataRepository;
import com.openrecordsmanager.database.DatabaseWritableProbe;
import com.openrecordsmanager.location.relationship.LocationRelationshipType;
import com.openrecordsmanager.location.type.LocationType;
import com.openrecordsmanager.location.LocationService;
import com.openrecordsmanager.location.user.User;
import com.openrecordsmanager.plugin.ExpressionsService;
import com.openrecordsmanager.plugin.registry.ComponentCatalog;
import com.openrecordsmanager.plugin.registry.TemplateComponentRegistry;
import com.openrecordsmanager.property.ObjectProperty;
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
    private final LocationService locationService;
    private final DataRepository repository;
    private final ComponentCatalog catalog;
    private final ExpressionsService expressions;
    private final AuditService auditService;
    private final DatabaseWritableProbe probe;

    public InitialDatabaseSeeder(
            SchemaMigrationState state,
            AuthService authService,
            LocationService locationService,
            DataRepository repository,
            ComponentCatalog catalog,
            ExpressionsService expressions,
            AuditService auditService,
            DatabaseWritableProbe probe
    ) {
        this.state = state;
        this.authService = authService;
        this.locationService = locationService;
        this.repository = repository;
        this.catalog = catalog;
        this.expressions = expressions;
        this.auditService = auditService;
        this.probe = probe;
    }

    @EventListener({ApplicationReadyEvent.class, SchemaMigrationReadyEvent.class})
    @Transactional
    public void seedIfNeeded() {
        // Re-probe before seeding: listener order vs DatabaseWritableProbe is not guaranteed,
        // and writable defaults to false until the first successful probe.
        this.probe.probe();
        if (!this.probe.isWritable()) {
            LOGGER.warn("Database is not writable on startup, data seeding will not occur");
            return;
        }

        AuditContext.disableCapture();
        try {
            this.seedBuiltinProperties();
            this.seedBuiltinLocationTypes();
            this.seedBuiltinRelationshipTypes();

            if (!this.state.consumeInitialSeedPending()) {
                return;
            }

            this.seedInitialAuth();
        } finally {
            AuditContext.clear();
        }
    }

    private void seedInitialAuth() {
        if (this.catalog.getRegistry(ComponentTypes.INPUT_AUTH_PROVIDER).get(LOCAL_AUTH_TYPE).isEmpty()) {
            LOGGER.warn("Skipping initial database seed: {} is not registered", LOCAL_AUTH_TYPE);
            return;
        }

        LOGGER.info("Seeding default local auth provider and admin user");

        AuthProviderResponse created = this.authService.createProvider(
                "Local Authentication",
                ComponentReference.of(ComponentTypes.INPUT_AUTH_PROVIDER, LOCAL_AUTH_TYPE),
                Map.of()
        );

        AuthProvider provider = this.repository.authProviderRepo.findById(created.id())
                .orElseThrow(() -> new IllegalStateException("Failed to load seeded auth provider"));

        LocationType userType = this.repository.locationTypeRepo
                .findById(new ResourceIdentifier(BuiltinPlugin.BUILTIN_PLUGIN_NAME, BuiltinLocationTypeIds.USER))
                .orElseThrow(() -> new IllegalStateException("Builtin location type 'user' is not registered"));

        User admin = new User("admin", provider, userType);
        this.repository.userRepo.saveAndFlush(admin);

        this.locationService.executeAction(
                admin,
                admin.getId(),
                RESET_PASSWORD_ACTION,
                Map.of("newPassword", "admin")
        );

        LOGGER.info("Seeded local auth provider {} and admin user {}", provider.getId(), admin.getId());
    }

    public void seedBuiltinProperties() {
        TemplateComponentRegistry<ObjectPropertyTemplate<?>, ObjectProperty<?>> registry =
                this.catalog.getTemplateRegistry(ComponentCatalog.OBJECT_PROPERTY_MAPPER);

        for (ResourceIdentifier id : registry.getIds()) {
            if (!id.isBuiltin()) {
                continue;
            }

            registry.register(
                    this.repository,
                    this.catalog,
                    this.expressions,
                    this.auditService,
                    ComponentReference.of(ComponentTypes.OBJECT_PROPERTY, id),
                    true
            );
        }
    }

    public void seedBuiltinLocationTypes() {
        TemplateComponentRegistry<LocationTypeTemplate, LocationType> registry =
                this.catalog.getTemplateRegistry(ComponentCatalog.LOCATION_TYPE_MAPPER);

        for (ResourceIdentifier id : registry.getIds()) {
            if (!id.isBuiltin()) {
                continue;
            }

            registry.register(
                    this.repository,
                    this.catalog,
                    this.expressions,
                    this.auditService,
                    ComponentReference.of(ComponentTypes.LOCATION_TYPE, id),
                    true
            );
        }
    }

    public void seedBuiltinRelationshipTypes() {
        TemplateComponentRegistry<LocationRelationshipTypeTemplate, LocationRelationshipType> registry =
                this.catalog.getTemplateRegistry(ComponentCatalog.LOCATION_RELATIONSHIP_TYPE_MAPPER);

        for (ResourceIdentifier id : registry.getIds()) {
            if (!id.isBuiltin()) {
                continue;
            }

            registry.register(
                    this.repository,
                    this.catalog,
                    this.expressions,
                    this.auditService,
                    ComponentReference.of(ComponentTypes.LOCATION_RELATIONSHIP_TYPE, id),
                    true
            );
        }
    }
}
