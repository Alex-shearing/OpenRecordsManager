package com.openrecordsmanager.property;

import com.openrecordsmanager.api.ResourceIdentifier;
import com.openrecordsmanager.api.errors.InputValidationException;
import com.openrecordsmanager.api.template.property.PropertyType;
import com.openrecordsmanager.api.types.ComponentTypes;
import com.openrecordsmanager.database.DataRepository;
import com.openrecordsmanager.list.ListType;
import com.openrecordsmanager.property.dto.NewObjectProperty;
import com.openrecordsmanager.property.dto.ObjectPropertyResponse;
import com.openrecordsmanager.property.dto.SimpleObjectPropertyResponse;
import com.openrecordsmanager.property.dto.UpdateObjectProperty;
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

    public ObjectPropertyService(DataRepository repository) {
        this.repository = repository;
    }

    @Transactional(readOnly = true)
    public Set<SimpleObjectPropertyResponse> getAll() {
        return this.repository.objectPropertyRepo.findAll().stream()
                .map(SimpleObjectPropertyResponse::of)
                .collect(Collectors.toSet());
    }

    @Transactional(readOnly = true)
    public ObjectPropertyResponse get(ResourceIdentifier id) throws ResourceNotFoundException {
        return this.repository.objectPropertyRepo.findById(id)
                .map(ObjectPropertyResponse::of)
                .orElseThrow(() -> new ResourceNotFoundException(ComponentTypes.OBJECT_PROPERTY, id));
    }

    @Transactional
    public ObjectPropertyResponse create(NewObjectProperty input) throws ResourceNotFoundException {
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

        return ObjectPropertyResponse.of(property);
    }

    @Transactional
    public ObjectPropertyResponse update(ResourceIdentifier id, UpdateObjectProperty input) throws ResourceNotFoundException {
        ObjectProperty<?> property = this.repository.objectPropertyRepo.findById(id)
                .orElseThrow(() -> new ResourceNotFoundException(ComponentTypes.OBJECT_PROPERTY, id));

        applyUpdate(property, input);

        this.repository.objectPropertyRepo.saveAndFlush(property);

        return ObjectPropertyResponse.of(property);
    }

    @Transactional
    public void delete(ResourceIdentifier id) throws ResourceNotFoundException, ResourceInUseException {
        ObjectProperty<?> property = this.repository.objectPropertyRepo.findById(id)
                .orElseThrow(() -> new ResourceNotFoundException(ComponentTypes.OBJECT_PROPERTY, id));

        if (this.repository.objectPropertyRepo.isAssignedToRecordType(property)
                || this.repository.objectPropertyRepo.isUsedByRecords(property)
                || this.repository.objectPropertyRepo.isUsedByUsers(property)) {
            throw new ResourceInUseException("object property is in use and cannot be deleted");
        }

        this.repository.objectPropertyRepo.delete(property);
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

    private static <T> void applyUpdate(ObjectProperty<T> property, UpdateObjectProperty input) {
        property.setName(input.name());
        property.setDescription(input.description());
        property.setValidator(input.validator());
        property.setSecurityFilter(input.securityFilter());
        property.setDefaultValue(property.getType().cast(input.defaultValue()));
        property.setUserHidden(input.userHidden());
    }
}
