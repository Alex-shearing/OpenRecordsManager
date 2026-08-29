package com.openrecordsmanager.property;

import com.openrecordsmanager.api.ResourceIdentifier;
import com.openrecordsmanager.api.audit.AuditEntityType;
import com.openrecordsmanager.api.audit.AuditOperation;
import com.openrecordsmanager.api.errors.InputValidationException;
import com.openrecordsmanager.api.template.property.PropertyType;
import com.openrecordsmanager.api.types.ComponentTypes;
import com.openrecordsmanager.audit.AuditEventDescriptions;
import com.openrecordsmanager.audit.AuditService;
import com.openrecordsmanager.audit.RequiresAuditComment;
import com.openrecordsmanager.database.DataRepository;
import com.openrecordsmanager.list.ListType;
import com.openrecordsmanager.property.dto.NewObjectPropertyRequest;
import com.openrecordsmanager.property.dto.ObjectPropertyResponse;
import com.openrecordsmanager.property.dto.SimpleObjectPropertyResponse;
import com.openrecordsmanager.property.dto.UpdateObjectPropertyRequest;
import com.openrecordsmanager.rest.errors.ResourceInUseException;
import com.openrecordsmanager.rest.errors.ResourceNotFoundException;
import org.jspecify.annotations.Nullable;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.Map;
import java.util.Set;
import java.util.stream.Collectors;

@Service
public class ObjectPropertyService {

    private final DataRepository repository;
    private final AuditService auditService;

    public ObjectPropertyService(DataRepository repository, AuditService auditService) {
        this.repository = repository;
        this.auditService = auditService;
    }

    @Transactional(readOnly = true)
    public Set<SimpleObjectPropertyResponse> getAll() {
        Set<SimpleObjectPropertyResponse> results = this.repository.objectPropertyRepo.findAll().stream()
                .map(SimpleObjectPropertyResponse::of)
                .collect(Collectors.toSet());
        this.auditService.recordCollectionRead(AuditEntityType.OBJECT_PROPERTY, results.size());
        return results;
    }

    @Transactional(readOnly = true)
    public ObjectPropertyResponse get(ResourceIdentifier id) throws ResourceNotFoundException {
        ObjectProperty<?> property = this.repository.objectPropertyRepo.findById(id)
                .orElseThrow(() -> new ResourceNotFoundException(ComponentTypes.OBJECT_PROPERTY, id));

        this.auditService.addReadEvent(AuditEntityType.OBJECT_PROPERTY, id);
        return ObjectPropertyResponse.of(property);
    }

    @Transactional
    @RequiresAuditComment(operation = AuditOperation.CREATE, targetType = AuditEntityType.OBJECT_PROPERTY)
    public ObjectPropertyResponse create(NewObjectPropertyRequest input) throws ResourceNotFoundException {
        if (this.repository.objectPropertyRepo.existsById(input.id())) {
            throw new ResourceInUseException("object property already exists: " + input.id());
        }

        ObjectProperty<?> property = buildProperty(
                input.id(),
                input.name(),
                input.description(),
                input.type(),
                input.listType(),
                input.validator(),
                input.securityFilter(),
                input.defaultValue(),
                input.userHidden()
        );

        this.repository.objectPropertyRepo.saveAndFlush(property);

        this.auditService.addEvent(AuditOperation.CREATE, AuditEntityType.OBJECT_PROPERTY, property.getId());

        return ObjectPropertyResponse.of(property);
    }

    @Transactional
    @RequiresAuditComment(operation = AuditOperation.UPDATE, targetType = AuditEntityType.OBJECT_PROPERTY)
    public ObjectPropertyResponse update(ResourceIdentifier id, UpdateObjectPropertyRequest input) throws ResourceNotFoundException {
        ObjectProperty<?> property = this.repository.objectPropertyRepo.findById(id)
                .orElseThrow(() -> new ResourceNotFoundException(ComponentTypes.OBJECT_PROPERTY, id));

        String oldName = property.getName();
        applyUpdate(property, input);

        this.repository.objectPropertyRepo.saveAndFlush(property);

        this.auditService.addEvent(
                AuditOperation.UPDATE,
                AuditEntityType.OBJECT_PROPERTY,
                id.toString(),
                AuditEventDescriptions.singleChange("name", oldName, input.name()),
                null,
                null
        );

        return ObjectPropertyResponse.of(property);
    }

    @Transactional
    @RequiresAuditComment(operation = AuditOperation.DELETE, targetType = AuditEntityType.OBJECT_PROPERTY)
    public void delete(ResourceIdentifier id) throws ResourceNotFoundException, ResourceInUseException {
        ObjectProperty<?> property = this.repository.objectPropertyRepo.findById(id)
                .orElseThrow(() -> new ResourceNotFoundException(ComponentTypes.OBJECT_PROPERTY, id));

        if (this.repository.objectPropertyRepo.isAssignedToRecordType(property)
                || this.repository.objectPropertyRepo.isUsedByRecords(property)
                || this.repository.objectPropertyRepo.isUsedByUsers(property)) {
            throw new ResourceInUseException("object property is in use and cannot be deleted");
        }

        this.repository.objectPropertyRepo.delete(property);

        this.auditService.addEvent(AuditOperation.DELETE, AuditEntityType.OBJECT_PROPERTY, id);
    }

    private ObjectProperty<?> buildProperty(
            ResourceIdentifier id,
            String name,
            String description,
            PropertyType<?> type,
            @Nullable ResourceIdentifier listTypeId,
            @Nullable String validator,
            @Nullable String securityFilter,
            @Nullable Object defaultValue,
            boolean userHidden
    ) {
        ListType listType = resolveListType(type, listTypeId);
        return createTypedProperty(id, name, description, type, listType, validator, securityFilter, defaultValue, userHidden);
    }

    private @Nullable ListType resolveListType(PropertyType<?> type, @Nullable ResourceIdentifier listTypeId) {
        if (!type.allowsList()) {
            if (listTypeId != null) {
                throw new InputValidationException(Map.of("listType", "listType can only be set for list property types"));
            }
            return null;
        }

        if (listTypeId == null) {
            throw new InputValidationException(Map.of("listType", "listType is required for list property types"));
        }

        return this.repository.listTypeRepo.findById(listTypeId)
                .orElseThrow(() -> new ResourceNotFoundException(ComponentTypes.LIST, listTypeId));
    }

    private static <T> ObjectProperty<T> createTypedProperty(
            ResourceIdentifier id,
            String name,
            String description,
            PropertyType<T> type,
            @Nullable ListType listType,
            @Nullable String validator,
            @Nullable String securityFilter,
            @Nullable Object defaultValue,
            boolean userHidden
    ) {
        return new ObjectProperty<>(
                id,
                name,
                description,
                type,
                listType,
                validator,
                securityFilter,
                type.cast(defaultValue),
                userHidden
        );
    }

    private static <T> void applyUpdate(ObjectProperty<T> property, UpdateObjectPropertyRequest input) {
        property.setName(input.name());
        property.setDescription(input.description());
        property.setValidator(input.validator());
        property.setSecurityFilter(input.securityFilter());
        property.setDefaultValue(property.getType().cast(input.defaultValue()));
        property.setUserHidden(input.userHidden());
    }
}
