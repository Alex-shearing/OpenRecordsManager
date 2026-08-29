package com.openrecordsmanager.plugin.registry.mapper;

import com.openrecordsmanager.api.Component;
import com.openrecordsmanager.api.ComponentReference;
import com.openrecordsmanager.api.ResourceIdentifier;
import com.openrecordsmanager.api.template.TemplateComponent;
import com.openrecordsmanager.api.types.ComponentType;
import com.openrecordsmanager.audit.AuditService;
import com.openrecordsmanager.database.DataRepository;
import com.openrecordsmanager.plugin.ExpressionsService;
import com.openrecordsmanager.plugin.registry.ComponentCatalog;
import com.openrecordsmanager.plugin.registry.TemplateComponentRegistry;

import java.util.HashSet;
import java.util.Optional;
import java.util.Set;

public abstract class TemplateRegistrationMapper<T extends TemplateComponent, D> {

    public abstract ComponentType<T> componentType();

    /**
     * Register the component template to the database
     *
     * @param repository          the data repository
     * @param catalog             the component catalog
     * @param expressions         the expressions service
     * @param auditService        the audit service
     * @param id                  id to register as
     * @param component           the component to register
     * @param includeDependencies if true, all dependencies of the component will be registered before registration
     */
    public final void register(
            DataRepository repository,
            ComponentCatalog catalog,
            ExpressionsService expressions,
            AuditService auditService,
            ResourceIdentifier id,
            T component,
            boolean includeDependencies
    ) {
        if (includeDependencies) {
            registerDependencies(repository, catalog, expressions, auditService, component);
        }

        // Validate all dependencies exist
        validateAllDependenciesRegistered(component, repository, catalog);

        this.register(repository, catalog, expressions, auditService, id, component);
    }

    /**
     * Register the dependencies of a component. Does not register the component itself,
     * see {@link TemplateRegistrationMapper#register(DataRepository, ComponentCatalog, ExpressionsService, AuditService, ResourceIdentifier, TemplateComponent, boolean)}.
     *
     * @param repository   the data repository
     * @param catalog      the component catalog
     * @param expressions  the expressions service
     * @param auditService the audit service
     * @param component    the component to register
     */
    public static void registerDependencies(
            DataRepository repository,
            ComponentCatalog catalog,
            ExpressionsService expressions,
            AuditService auditService,
            Component component
    ) {
        for (ComponentReference<? extends TemplateComponent> dependencyRef : component.getDependencies()) {
            registerDependency(repository, catalog, expressions, auditService, dependencyRef);
        }
    }

    private static <K extends TemplateComponent> void registerDependency(
            DataRepository repository,
            ComponentCatalog catalog,
            ExpressionsService expressions,
            AuditService auditService,
            ComponentReference<K> reference
    ) {
        catalog.getTemplateRegistry(ComponentCatalog.mapperFromComponent(reference.getType()))
                .register(
                        repository,
                        catalog,
                        expressions,
                        auditService,
                        reference,
                        true
                );
    }

    /**
     * Register the component template to the database
     *
     * @param repository   the data repository
     * @param catalog      the component catalog
     * @param expressions  expressions service
     * @param auditService the audit service
     * @param id           id to register as
     * @param component    the component
     */
    protected abstract void register(
            DataRepository repository,
            ComponentCatalog catalog,
            ExpressionsService expressions,
            AuditService auditService,
            ResourceIdentifier id,
            T component
    );

    /**
     * Get the registered instance of the component from the database
     *
     * @param id   the component id
     * @param repo the data repository
     * @return an optional instance of the registered component
     */
    public abstract Optional<D> getRegistered(ResourceIdentifier id, DataRepository repo);

    /**
     * Validates that all the dependencies of the component are registered, including the dependencies of dependencies.
     *
     * @param component  the component to confirm.
     * @param repository data repository
     * @param catalog    resource catalog
     */
    protected static void validateAllDependenciesRegistered(
            Component component,
            DataRepository repository,
            ComponentCatalog catalog
    ) {
        for (ComponentReference<? extends TemplateComponent> dependency : collectDependencies(component, new HashSet<>())) {
            ResourceIdentifier dependencyId = dependency.getId(catalog)
                    .orElseThrow(() -> new IllegalStateException("Cannot find dependency " + dependency));

            TemplateComponentRegistry<?, ?> registry = catalog.getTemplateRegistry(ComponentCatalog.mapperFromComponent(dependency.getType()));

            Optional<?> childComponent = registry.getRegistered(dependencyId, repository);
            if (childComponent.isEmpty()) {
                throw new IllegalStateException("Unregistered dependency " + dependencyId);
            }
        }
    }

    /**
     * Recursively collect all dependencies of a component.
     *
     * @param component    the component to collect from
     * @param dependencies the current list of dependencies
     * @return the input set, with new dependencies added
     */
    private static Set<ComponentReference<? extends TemplateComponent>> collectDependencies(Component component, Set<ComponentReference<? extends TemplateComponent>> dependencies) {
        for (ComponentReference<? extends TemplateComponent> dependencyRef : component.getDependencies()) {
            if (!dependencies.contains(dependencyRef)) {
                dependencies.add(dependencyRef);
                dependencies.addAll(collectDependencies(component, dependencies));
            }
        }
        return dependencies;
    }
}
