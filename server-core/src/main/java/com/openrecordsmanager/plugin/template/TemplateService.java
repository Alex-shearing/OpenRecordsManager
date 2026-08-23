package com.openrecordsmanager.plugin.template;

import com.openrecordsmanager.api.ComponentReference;
import com.openrecordsmanager.api.ResourceIdentifier;
import com.openrecordsmanager.api.template.TemplateComponent;
import com.openrecordsmanager.api.types.ComponentType;
import com.openrecordsmanager.database.DataRepository;
import com.openrecordsmanager.plugin.ExpressionsService;
import com.openrecordsmanager.plugin.registry.ComponentCatalog;
import com.openrecordsmanager.plugin.registry.TemplateComponentRegistry;
import com.openrecordsmanager.plugin.registry.mapper.TemplateRegistrationMapper;
import com.openrecordsmanager.rest.errors.ResourceNotFoundException;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.Set;
import java.util.stream.Collectors;

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

    public Set<String> listTemplateTypes() {
        return this.catalog.getTemplateTypes().stream()
                .map(ComponentType::toString)
                .collect(Collectors.toSet());
    }

    public Set<ResourceIdentifier> listTemplates(String typeName) {
        return this.catalog.getTemplateRegistry(resolveMapper(typeName)).getIds();
    }

    public TemplateComponent getTemplate(String typeName, ResourceIdentifier templateId) {
        TemplateRegistrationMapper<?, ?> type = resolveMapper(typeName);
        TemplateComponentRegistry<?, ?> registry = this.catalog.getTemplateRegistry(type);

        return registry.get(templateId)
                .orElseThrow(() -> new ResourceNotFoundException(type.componentType(), templateId));
    }

    @Transactional
    public void registerTemplate(
            String typeName,
            ResourceIdentifier templateId,
            boolean includeDependencies
    ) {
        registerTyped(resolveMapper(typeName), templateId, includeDependencies);
    }

    private <T extends TemplateComponent> void registerTyped(
            TemplateRegistrationMapper<T, ?> type,
            ResourceIdentifier templateId,
            boolean includeDependencies
    ) {
        TemplateComponentRegistry<T, ?> registry = this.catalog.getTemplateRegistry(type);
        ComponentReference<T> ref = ComponentReference.of(type.componentType(), templateId);
        registry.register(this.repository, this.catalog, this.expressions, ref, includeDependencies);
    }

    private TemplateRegistrationMapper<?, ?> resolveMapper(String typeName) {
        TemplateRegistrationMapper<?, ?> type = ComponentCatalog.mapperFromName(typeName);
        if (type == null) {
            throw new ResourceNotFoundException("template type", typeName);
        }
        return type;
    }
}
