package com.openrecordsmanager.template;

import com.openrecordsmanager.api.ComponentReference;
import com.openrecordsmanager.api.ResourceIdentifier;
import com.openrecordsmanager.api.audit.AuditEntityType;
import com.openrecordsmanager.api.audit.AuditOperation;
import com.openrecordsmanager.api.template.TemplateComponent;
import com.openrecordsmanager.audit.AuditContext;
import com.openrecordsmanager.audit.AuditPolicyService;
import com.openrecordsmanager.audit.AuditService;
import com.openrecordsmanager.database.DataRepository;
import com.openrecordsmanager.plugin.ExpressionsService;
import com.openrecordsmanager.plugin.registry.ComponentCatalog;
import com.openrecordsmanager.plugin.registry.TemplateComponentRegistry;
import com.openrecordsmanager.plugin.registry.mapper.TemplateRegistrationMapper;
import com.openrecordsmanager.rest.dto.ComponentReferenceDto;
import com.openrecordsmanager.rest.exception.ResourceNotFoundException;
import com.openrecordsmanager.template.dto.TemplateType;
import org.jspecify.annotations.Nullable;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.Set;
import java.util.stream.Collectors;
import java.util.stream.Stream;

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
    public Set<ComponentReferenceDto> listTemplates(@Nullable TemplateType type) {
        Set<ComponentReferenceDto> templates = (type == null
                ? ComponentCatalog.TEMPLATE_MAPPERS.stream()
                : Stream.of(ComponentCatalog.mapperFromComponent(type.componentType())))
                .flatMap(mapper ->
                        this.catalog.getTemplateRegistry(mapper).getIds().stream()
                                .map(id -> ComponentReferenceDto.of(mapper.componentType(), id)))
                .collect(Collectors.toSet());

        this.auditService.recordCollectionRead(AuditEntityType.TEMPLATE, templates.size());
        return templates;
    }

    @Transactional(readOnly = true)
    public TemplateComponent getTemplate(TemplateType type, ResourceIdentifier templateId) {
        TemplateRegistrationMapper<?, ?> mapper = ComponentCatalog.mapperFromComponent(type.componentType());
        TemplateComponentRegistry<?, ?> registry = this.catalog.getTemplateRegistry(mapper);

        TemplateComponent template = registry.get(templateId)
                .orElseThrow(() -> new ResourceNotFoundException(mapper.componentType(), templateId));

        this.auditService.addReadEvent(AuditEntityType.TEMPLATE, templateId);
        return template;
    }

    @Transactional
    public void registerTemplate(
            TemplateType type,
            ResourceIdentifier templateId,
            boolean includeDependencies
    ) {
        TemplateRegistrationMapper<?, ?> mapper = ComponentCatalog.mapperFromComponent(type.componentType());

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
        registry.register(
                this.repository,
                this.catalog,
                this.expressions,
                this.auditService,
                ref,
                includeDependencies
        );
    }
}
