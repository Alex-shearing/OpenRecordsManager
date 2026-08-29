package com.openrecordsmanager.plugin.registry.mapper;

import com.openrecordsmanager.api.ResourceIdentifier;
import com.openrecordsmanager.api.template.list.ListTemplate;
import com.openrecordsmanager.api.types.ComponentType;
import com.openrecordsmanager.api.types.ComponentTypes;
import com.openrecordsmanager.audit.AuditService;
import com.openrecordsmanager.database.DataRepository;
import com.openrecordsmanager.list.ListType;
import com.openrecordsmanager.plugin.ExpressionsService;
import com.openrecordsmanager.plugin.registry.ComponentCatalog;

import java.util.Optional;

public class ListTemplateRegistrationMapper extends TemplateRegistrationMapper<ListTemplate, ListType> {

    @Override
    public ComponentType<ListTemplate> componentType() {
        return ComponentTypes.LIST;
    }

    @Override
    public void register(
            DataRepository repository,
            ComponentCatalog catalog,
            ExpressionsService expressions,
            AuditService auditService,
            ResourceIdentifier id,
            ListTemplate component
    ) {
        ListType type = new ListType(id, component.name());
        repository.listTypeRepo.saveAndFlush(type);

        component.defaultEntries().forEach((s, listItem) -> {
            ResourceIdentifier childId = new ResourceIdentifier(id.source(), s);

            catalog.getTemplateRegistry(ComponentCatalog.LIST_ELEMENT_MAPPER)
                    .register(repository, catalog, expressions, auditService, childId, listItem, false);
        });

    }

    @Override
    public Optional<ListType> getRegistered(ResourceIdentifier id, DataRepository repo) {
        return repo.listTypeRepo.findById(id);
    }
}
