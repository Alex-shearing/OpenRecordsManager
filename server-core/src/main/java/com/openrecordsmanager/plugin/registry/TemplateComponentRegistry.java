package com.openrecordsmanager.plugin.registry;

import com.openrecordsmanager.api.Component;
import com.openrecordsmanager.api.ResourceIdentifier;
import com.openrecordsmanager.database.DataRepository;
import com.openrecordsmanager.plugin.ExpressionsService;
import com.openrecordsmanager.plugin.registry.mapper.ComponentRegistrationMapper;

import java.util.Optional;

public class TemplateComponentRegistry<T extends Component, D> extends ComponentRegistry<T> {
    private final ComponentRegistrationMapper<T, D> mapper;

    public TemplateComponentRegistry(ComponentRegistrationMapper<T, D> mapper) {
        this.mapper = mapper;
    }

    public void register(DataRepository repository, ComponentCatalog catalog, ExpressionsService expressions, ResourceIdentifier templateId, T template, boolean includeDependencies) {
        this.mapper.register(repository, catalog, expressions, templateId, template, includeDependencies);
    }

    public Optional<D> getRegistered(ResourceIdentifier id, DataRepository repository) {
        return this.mapper.getRegistered(id, repository);
    }
}
