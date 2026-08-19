package com.openrecordsmanager.plugin.template;

import com.openrecordsmanager.api.ComponentReference;
import com.openrecordsmanager.api.ResourceIdentifier;
import com.openrecordsmanager.api.template.TemplateComponent;
import com.openrecordsmanager.api.types.ComponentType;
import com.openrecordsmanager.database.DataRepository;
import com.openrecordsmanager.plugin.ExpressionsService;
import com.openrecordsmanager.plugin.registry.ComponentCatalog;
import com.openrecordsmanager.plugin.registry.TemplateComponentRegistry;
import org.springframework.stereotype.Service;

@Service
public class TemplateService {

    private final ComponentCatalog catalog;
    private final DataRepository repository;
    private final ExpressionsService expressions;

    public TemplateService(ComponentCatalog catalog, DataRepository repository, ExpressionsService expressions) {
        this.catalog = catalog;
        this.repository = repository;
        this.expressions = expressions;
    }

    public <T extends TemplateComponent> void registerTemplate(
            ComponentType<T> type,
            ResourceIdentifier templateId,
            boolean includeDependencies
    ) {
        TemplateComponentRegistry<T, ?> registry = this.catalog.getTemplateRegistry(type);
        registry.register(this.repository, this.catalog, this.expressions, ComponentReference.of(type, templateId), includeDependencies);
    }
}
