package com.openrecordsmanager.plugin.template;

import com.openrecordsmanager.api.Component;
import com.openrecordsmanager.api.ResourceIdentifier;
import com.openrecordsmanager.api.errors.ResourceNotFoundException;
import com.openrecordsmanager.api.types.ComponentType;
import com.openrecordsmanager.api.types.ComponentTypes;
import com.openrecordsmanager.database.DataRepository;
import com.openrecordsmanager.plugin.ComponentCatalog;
import com.openrecordsmanager.plugin.ExpressionsService;
import com.openrecordsmanager.plugin.TemplateComponentRegistry;
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

    public <T extends Component> void registerTemplate(
            ComponentType<T> type,
            ResourceIdentifier templateId,
            boolean includeDependencies
    ) {
        TemplateComponentRegistry<T, ?> registry = this.catalog.getTemplateRegistry(type);

        T template = registry.getComponent(templateId)
                .orElseThrow(() -> new ResourceNotFoundException(ComponentTypes.RECORD_TYPE, templateId));

        registry.register(this.repository, this.catalog, this.expressions, templateId, template, includeDependencies);
    }
}
