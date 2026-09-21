package com.openrecordsmanager.rest.dto;

import com.openrecordsmanager.api.Component;
import com.openrecordsmanager.api.ComponentAccess;
import com.openrecordsmanager.api.ComponentReference;
import com.openrecordsmanager.api.ResourceIdentifier;
import com.openrecordsmanager.api.errors.InputValidationException;
import com.openrecordsmanager.api.types.ComponentType;
import com.openrecordsmanager.api.types.ComponentTypes;
import io.swagger.v3.oas.annotations.media.Schema;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;

import java.util.Map;

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
            throw new InputValidationException(Map.of(
                    "type",
                    String.format("Unknown component type '%s'", this.type)
            ));
        }
        return ComponentReference.of(componentType, this.id);
    }
}
