package com.openrecordsmanager.template;

import com.openrecordsmanager.api.ComponentReference;
import com.openrecordsmanager.api.ResourceIdentifier;
import com.openrecordsmanager.api.audit.AuditEntityType;
import com.openrecordsmanager.api.audit.AuditOperation;
import com.openrecordsmanager.api.template.TemplateComponent;
import com.openrecordsmanager.api.types.ComponentType;
import com.openrecordsmanager.audit.AuditContext;
import com.openrecordsmanager.audit.AuditPolicyService;
import com.openrecordsmanager.audit.AuditService;
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
    private final AuditService auditService;
    private final AuditPolicyService auditPolicyService;

    public TemplateService(
            ComponentCatalog catalog,
            DataRepository repository,
            ExpressionsService expressions,
            AuditService auditService,
            AuditPolicyService auditPolicyService
    ) {
        this.catalog = catalog;
        this.repository = repository;
        this.expressions = expressions;
        this.auditService = auditService;
        this.auditPolicyService = auditPolicyService;
    }

    @Transactional(readOnly = true)
    public Set<String> listTemplateTypes() {
        return this.catalog.getTemplateTypes().stream()
                .map(ComponentType::toString)
                .collect(Collectors.toSet());
    }

    @Transactional(readOnly = true)
    public Set<ResourceIdentifier> listTemplates(String typeName) {
        TemplateRegistrationMapper<?, ?> mapper = resolveMapper(typeName);
        Set<ResourceIdentifier> ids = this.catalog.getTemplateRegistry(mapper).getIds();
        this.auditService.recordCollectionRead(
                AuditEntityType.fromComponentType(mapper.componentType()),
                ids.size()
        );
        return ids;
    }

    @Transactional(readOnly = true)
    public TemplateComponent getTemplate(String typeName, ResourceIdentifier templateId) {
        TemplateRegistrationMapper<?, ?> type = resolveMapper(typeName);
        TemplateComponentRegistry<?, ?> registry = this.catalog.getTemplateRegistry(type);

        TemplateComponent template = registry.get(templateId)
                .orElseThrow(() -> new ResourceNotFoundException(type.componentType(), templateId));

        this.auditService.addReadEvent(AuditEntityType.fromComponentType(type.componentType()), templateId);
        return template;
    }

    @Transactional
    public void registerTemplate(
            String typeName,
            ResourceIdentifier templateId,
            boolean includeDependencies
    ) {
        TemplateRegistrationMapper<?, ?> mapper = this.resolveMapper(typeName);

        // Audit logic
        AuditEntityType targetType = AuditEntityType.fromComponentType(mapper.componentType());
        if (AuditContext.isCaptureEnabled()) {
            this.auditPolicyService.validateCommentRequired(targetType, AuditOperation.CREATE);
        }

        this.registerTyped(mapper, templateId, includeDependencies);

        this.auditService.addEvent(AuditOperation.CREATE, targetType, templateId);
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
