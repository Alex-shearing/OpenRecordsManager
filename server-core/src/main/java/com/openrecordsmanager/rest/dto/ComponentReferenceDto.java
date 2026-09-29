package com.openrecordsmanager.rest.dto;

import com.openrecordsmanager.api.Component;
import com.openrecordsmanager.api.ComponentAccess;
import com.openrecordsmanager.api.ComponentReference;
import com.openrecordsmanager.api.ResourceIdentifier;
import com.openrecordsmanager.api.types.ComponentType;
import com.openrecordsmanager.api.types.ComponentTypes;
import com.openrecordsmanager.rest.exception.ResourceNotFoundException;
import io.swagger.v3.oas.annotations.media.Schema;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;


@Schema(name = "ComponentReference")
public record ComponentReferenceDto(
        @NotNull ResourceIdentifier id,
        @NotBlank String type
) {
    public static ComponentReferenceDto of(ComponentAccess catalog, ComponentReference<?> reference) {
        return new ComponentReferenceDto(
                reference.getId(catalog).orElseThrow(),
                reference.getType().name()
        );
    }

    public <T extends Component> ComponentReference<T> toReference() {
        @SuppressWarnings("unchecked")
        ComponentType<T> componentType = (ComponentType<T>) ComponentTypes.fromName(this.type);

        if (componentType == null) {
            throw new ResourceNotFoundException("component type", this.type);
        }
        return ComponentReference.of(componentType, this.id);
    }
}
