package com.openrecordsmanager.database.schema;

import com.openrecordsmanager.api.template.TemplateComponent;
import com.openrecordsmanager.audit.AuditContext;
import com.openrecordsmanager.audit.AuditService;
import com.openrecordsmanager.database.DataRepository;
import com.openrecordsmanager.database.DatabaseWritableProbe;
import com.openrecordsmanager.plugin.ExpressionsService;
import com.openrecordsmanager.plugin.registry.ComponentCatalog;
import com.openrecordsmanager.plugin.registry.TemplateComponentRegistry;
import com.openrecordsmanager.plugin.registry.mapper.TemplateRegistrationMapper;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.boot.context.event.ApplicationReadyEvent;
import org.springframework.context.event.EventListener;
import org.springframework.stereotype.Component;
import org.springframework.transaction.annotation.Transactional;

/**
 * Seeds builtin templates. Called during first-run setup and again after schema upgrades
 * on installs that already have users.
 */
@Component
public class BuiltinTemplateSeeder {
    private static final Logger LOGGER = LoggerFactory.getLogger(BuiltinTemplateSeeder.class);

    private final DataRepository repository;
    private final ComponentCatalog catalog;
    private final ExpressionsService expressions;
    private final AuditService auditService;
    private final DatabaseWritableProbe probe;

    public BuiltinTemplateSeeder(
            DataRepository repository,
            ComponentCatalog catalog,
            ExpressionsService expressions,
            AuditService auditService,
            DatabaseWritableProbe probe
    ) {
        this.repository = repository;
        this.catalog = catalog;
        this.expressions = expressions;
        this.auditService = auditService;
        this.probe = probe;
    }

    @EventListener({ApplicationReadyEvent.class, SchemaMigrationReadyEvent.class})
    @Transactional
    public void seedIfNeeded() {
        this.probe.probe();
        if (!this.probe.isWritable()) {
            LOGGER.warn("Database is not writable on startup, builtin template seeding will not occur");
            return;
        }

        // First-run installs seed builtins during POST /api/setup/complete.
        if (this.repository.userRepo.count() == 0) {
            LOGGER.info("Skipping builtin template seed until first-run setup completes");
            return;
        }

        AuditContext.disableCapture();
        try {
            this.seedAll();
        } finally {
            AuditContext.clear();
        }
    }

    public void seedAll() {
        this.seedBuiltin(ComponentCatalog.OBJECT_PROPERTY_MAPPER);
        this.seedBuiltin(ComponentCatalog.LOCATION_TYPE_MAPPER);
        this.seedBuiltin(ComponentCatalog.LOCATION_RELATIONSHIP_TYPE_MAPPER);
    }

    public <T extends TemplateComponent, D> void seedBuiltin(TemplateRegistrationMapper<T, D> mapper) {
        TemplateComponentRegistry<T, D> registry = this.catalog.getTemplateRegistry(mapper);

        registry.references()
                .filter(ref -> ref.id().isBuiltin())
                .forEach(ref -> registry.register(
                        this.repository,
                        this.catalog,
                        this.expressions,
                        this.auditService,
                        ref,
                        true
                ));
    }
}
