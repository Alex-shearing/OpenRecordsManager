package com.openrecordsmanager.plugin.registry;

import com.openrecordsmanager.api.ComponentReference;
import com.openrecordsmanager.api.ResourceIdentifier;
import com.openrecordsmanager.api.audit.AuditEntityType;
import com.openrecordsmanager.api.audit.AuditOperation;
import com.openrecordsmanager.api.template.TemplateComponent;
import com.openrecordsmanager.audit.AuditService;
import com.openrecordsmanager.database.DataRepository;
import com.openrecordsmanager.plugin.ExpressionsService;
import com.openrecordsmanager.plugin.registry.mapper.TemplateRegistrationMapper;

import java.util.Optional;

public class TemplateComponentRegistry<T extends TemplateComponent, D> extends ComponentRegistry<T> {
    private final TemplateRegistrationMapper<T, D> mapper;

    public TemplateComponentRegistry(TemplateRegistrationMapper<T, D> mapper) {
        this.mapper = mapper;
    }

    public void register(
            DataRepository repository,
            ComponentCatalog catalog,
            ExpressionsService expressions,
            AuditService auditService,
            ResourceIdentifier templateId,
            T template,
            boolean includeDependencies
    ) {
        this.mapper.register(repository, catalog, expressions, auditService, templateId, template, includeDependencies);

        auditService.addEvent(
                AuditOperation.CREATE,
                AuditEntityType.fromComponentType(this.mapper.componentType()),
                templateId
        );
    }

    public void register(
            DataRepository repository,
            ComponentCatalog catalog,
            ExpressionsService expressions,
            AuditService auditService,
            ComponentReference<T> reference,
            boolean includeDependencies
    ) {
        ResourceIdentifier dependencyId = reference.getId(catalog)
                .orElseThrow(() -> new IllegalStateException("Cannot find dependency " + reference));

        T template = reference.getComponent(catalog)
                .orElseThrow(() -> new IllegalStateException("Cannot find referenced dependency " + reference));

        this.register(
                repository,
                catalog,
                expressions,
                auditService,
                dependencyId,
                template,
                includeDependencies
        );
    }

    public Optional<D> getRegistered(ResourceIdentifier id, DataRepository repository) {
        return this.mapper.getRegistered(id, repository);
    }

    public Optional<D> getRegistered(T component, DataRepository repository) {
        return this.getId(component).flatMap(id -> this.mapper.getRegistered(id, repository));
    }
}
