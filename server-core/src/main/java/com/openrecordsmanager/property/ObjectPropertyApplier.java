package com.openrecordsmanager.property;

import com.openrecordsmanager.api.ResourceIdentifier;
import com.openrecordsmanager.api.types.ComponentTypes;
import com.openrecordsmanager.audit.AuditPropertyChange;
import com.openrecordsmanager.database.DataRepository;
import com.openrecordsmanager.rest.errors.ResourceNotFoundException;
import org.springframework.stereotype.Component;
import tools.jackson.databind.JsonNode;

import java.util.List;
import java.util.Map;
import java.util.Objects;

/**
 * Shared apply path for user/record property maps from the API (wire {@link JsonNode}).
 */
@Component
public class ObjectPropertyApplier {

    private final DataRepository repository;
    private final PropertyValueCodec codec;

    public ObjectPropertyApplier(DataRepository repository, PropertyValueCodec codec) {
        this.repository = repository;
        this.codec = codec;
    }

    public void applyOnCreate(
            ObjectPropertyHolder<?, ?> holder,
            Map<ResourceIdentifier, JsonNode> properties,
            boolean excludeUserHidden,
            List<AuditPropertyChange> changes
    ) {
        properties.forEach((identifier, value) -> {
            ObjectProperty<?> property = findProperty(identifier, excludeUserHidden);
            Object newValue = holder.setPropertyFromJson(property, value);
            changes.add(AuditPropertyChange.newProperty(
                    identifier.toString(),
                    this.codec.encode(property, newValue)
            ));
        });
    }

    public void applyOnUpdate(
            ObjectPropertyHolder<?, ?> holder,
            Map<ResourceIdentifier, JsonNode> properties,
            boolean excludeUserHidden,
            List<AuditPropertyChange> changes
    ) {
        properties.forEach((identifier, value) -> {
            ObjectProperty<?> property = findProperty(identifier, excludeUserHidden);
            Object oldValue = holder.getProperty(property);
            Object newValue = holder.setPropertyFromJson(property, value);
            if (!Objects.equals(oldValue, newValue)) {
                changes.add(AuditPropertyChange.of(
                        identifier.toString(),
                        this.codec.encode(property, oldValue),
                        this.codec.encode(property, newValue)
                ));
            }
        });
    }

    private ObjectProperty<?> findProperty(ResourceIdentifier identifier, boolean excludeUserHidden) {
        return this.repository.objectPropertyRepo.findById(identifier)
                .filter(p -> !excludeUserHidden || !p.isUserHidden())
                .orElseThrow(() -> new ResourceNotFoundException(ComponentTypes.OBJECT_PROPERTY, identifier));
    }
}
