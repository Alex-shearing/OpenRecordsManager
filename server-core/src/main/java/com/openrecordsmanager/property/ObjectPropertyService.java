package com.openrecordsmanager.property;

import com.openrecordsmanager.api.ComponentReference;
import com.openrecordsmanager.api.ResourceIdentifier;
import com.openrecordsmanager.api.TranslationField;
import com.openrecordsmanager.api.audit.AuditEntityType;
import com.openrecordsmanager.api.audit.AuditOperation;
import com.openrecordsmanager.api.errors.ApiException;
import com.openrecordsmanager.api.template.property.PropertyType;
import com.openrecordsmanager.api.types.ComponentTypes;
import com.openrecordsmanager.audit.AuditEventDescriptions;
import com.openrecordsmanager.audit.AuditService;
import com.openrecordsmanager.audit.RequiresAuditComment;
import com.openrecordsmanager.config.ConfigValueParseFailedException;
import com.openrecordsmanager.database.DataRepository;
import com.openrecordsmanager.i18n.TranslationOverrideService;
import com.openrecordsmanager.list.ListType;
import com.openrecordsmanager.plugin.exception.BuiltinResourceImmutableException;
import com.openrecordsmanager.property.dto.NewObjectPropertyRequest;
import com.openrecordsmanager.property.dto.ObjectPropertyResponse;
import com.openrecordsmanager.property.dto.SimpleObjectPropertyResponse;
import com.openrecordsmanager.property.dto.UpdateObjectPropertyRequest;
import com.openrecordsmanager.rest.exception.ResourceAlreadyExistsException;
import com.openrecordsmanager.rest.exception.ResourceInUseException;
import com.openrecordsmanager.rest.exception.ResourceNotFoundException;
import org.jspecify.annotations.Nullable;
import org.springframework.context.i18n.LocaleContextHolder;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import tools.jackson.databind.JsonNode;

import java.util.Locale;
import java.util.Set;
import java.util.stream.Collectors;

@Service
public class ObjectPropertyService {
    private static final String LIST_TYPE_ONLY_FOR_LIST_PROPERTY = "list_type_only_for_list_property";
    private static final String LIST_TYPE_REQUIRED_FOR_LIST_PROPERTY = "list_type_required_for_list_property";

    private final DataRepository repository;
    private final AuditService auditService;
    private final TranslationOverrideService overrideService;

    public ObjectPropertyService(
            DataRepository repository,
            AuditService auditService,
            TranslationOverrideService overrideService
    ) {
        this.repository = repository;
        this.auditService = auditService;
        this.overrideService = overrideService;
    }

    @Transactional(readOnly = true)
    public Set<SimpleObjectPropertyResponse> getAll() {
        Set<SimpleObjectPropertyResponse> results = this.repository.objectPropertyRepo.findAll().stream()
                .filter(property -> !property.isUserHidden())
                .map(SimpleObjectPropertyResponse::of)
                .collect(Collectors.toSet());
        this.auditService.recordCollectionRead(AuditEntityType.OBJECT_PROPERTY, results.size());
        return results;
    }

    @Transactional(readOnly = true)
    public ObjectPropertyResponse get(ResourceIdentifier id) throws ResourceNotFoundException {
        ObjectProperty<?> property = this.repository.objectPropertyRepo.findById(id)
                .filter(p -> !p.isUserHidden())
                .orElseThrow(() -> new ResourceNotFoundException(ComponentTypes.OBJECT_PROPERTY, id));

        this.auditService.addReadEvent(AuditEntityType.OBJECT_PROPERTY, id);
        return ObjectPropertyResponse.of(property);
    }

    @Transactional
    @RequiresAuditComment(operation = AuditOperation.CREATE, targetType = AuditEntityType.OBJECT_PROPERTY)
    public ObjectPropertyResponse create(NewObjectPropertyRequest input) throws ResourceNotFoundException {
        if (this.repository.objectPropertyRepo.existsById(input.id())) {
            throw new ResourceAlreadyExistsException(ComponentTypes.OBJECT_PROPERTY, input.id());
        }

        if (input.id().isBuiltin()) {
            throw new BuiltinResourceImmutableException();
        }

        ObjectProperty<?> property = buildProperty(
                input.id(),
                input.type(),
                input.listType(),
                input.validator(),
                input.securityFilter(),
                input.defaultValue()
        );

        ComponentReference.Reference<?> ref = property.getReference();

        this.repository.objectPropertyRepo.saveAndFlush(property);

        Locale locale = LocaleContextHolder.getLocale();
        this.overrideService.upsertSilent(ref.getTranslationKey(TranslationField.NAME), locale, input.name());
        this.overrideService.upsertSilent(ref.getTranslationKey(TranslationField.DESCRIPTION), locale, input.description());

        this.auditService.addEvent(AuditOperation.CREATE, AuditEntityType.OBJECT_PROPERTY, property.getId());

        return ObjectPropertyResponse.of(property);
    }

    @Transactional
    @RequiresAuditComment(operation = AuditOperation.UPDATE, targetType = AuditEntityType.OBJECT_PROPERTY)
    public ObjectPropertyResponse update(ResourceIdentifier id, UpdateObjectPropertyRequest input) throws ResourceNotFoundException {
        ObjectProperty<?> property = this.repository.objectPropertyRepo.findById(id)
                .filter(p -> !p.isUserHidden())
                .orElseThrow(() -> new ResourceNotFoundException(ComponentTypes.OBJECT_PROPERTY, id));

        if (id.isBuiltin()) {
            throw new BuiltinResourceImmutableException();
        }

        ComponentReference.Reference<?> ref = property.getReference();

        applyUpdate(property, input);
        Locale locale = LocaleContextHolder.getLocale();
        String oldName = this.overrideService.upsertSilent(ref.getTranslationKey(TranslationField.NAME), locale, input.name());
        this.overrideService.upsertSilent(ref.getTranslationKey(TranslationField.DESCRIPTION), locale, input.description());

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
                .filter(p -> !p.isUserHidden())
                .orElseThrow(() -> new ResourceNotFoundException(ComponentTypes.OBJECT_PROPERTY, id));

        if (id.isBuiltin()) {
            throw new BuiltinResourceImmutableException();
        }

        if (this.repository.objectPropertyRepo.isAssignedToRecordType(property)
                || this.repository.objectPropertyRepo.isUsedByRecords(property)
                || this.repository.objectPropertyRepo.isUsedByUsers(property)) {
            throw new ResourceInUseException(ComponentTypes.OBJECT_PROPERTY);
        }

        this.repository.objectPropertyRepo.delete(property);

        this.auditService.addEvent(AuditOperation.DELETE, AuditEntityType.OBJECT_PROPERTY, id);
    }

    private <T> ObjectProperty<T> buildProperty(
            ResourceIdentifier id,
            PropertyType<T> type,
            @Nullable ResourceIdentifier listTypeId,
            @Nullable String validator,
            @Nullable String securityFilter,
            @Nullable JsonNode defaultValue
    ) {
        ListType listType = resolveListType(type, listTypeId);
        if (defaultValue != null && !defaultValue.isNull() && type.parse(defaultValue) == null) {
            throw ApiException.validationFailed(
                    "defaultValue",
                    ConfigValueParseFailedException.CODE,
                    type.getName(),
                    defaultValue.toString()
            );
        }
        return new ObjectProperty<>(
                id,
                type,
                listType,
                validator,
                securityFilter,
                defaultValue,
                false
        );
    }

    private @Nullable ListType resolveListType(PropertyType<?> type, @Nullable ResourceIdentifier listTypeId) {
        if (!type.allowsList()) {
            if (listTypeId != null) {
                throw ApiException.validationFailed("listType", LIST_TYPE_ONLY_FOR_LIST_PROPERTY);
            }
            return null;
        }

        if (listTypeId == null) {
            throw ApiException.validationFailed("listType", LIST_TYPE_REQUIRED_FOR_LIST_PROPERTY);
        }

        return this.repository.listTypeRepo.findById(listTypeId)
                .orElseThrow(() -> new ResourceNotFoundException(ComponentTypes.LIST, listTypeId));
    }

    private static <T> void applyUpdate(ObjectProperty<T> property, UpdateObjectPropertyRequest input) {
        property.setValidator(input.validator());
        property.setSecurityFilter(input.securityFilter());
        JsonNode defaultValue = input.defaultValue();
        if (defaultValue != null && !defaultValue.isNull() && property.getType().parse(defaultValue) == null) {
            throw ApiException.validationFailed(
                    "defaultValue",
                    ConfigValueParseFailedException.CODE,
                    property.getType().getName(),
                    defaultValue.toString()
            );
        }
        property.setDefaultValue(defaultValue);
    }
}
