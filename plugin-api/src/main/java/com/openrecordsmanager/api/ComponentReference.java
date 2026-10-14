package com.openrecordsmanager.api;

import com.fasterxml.jackson.annotation.JsonCreator;
import com.openrecordsmanager.api.types.ComponentType;
import com.openrecordsmanager.api.types.ComponentTypes;
import tools.jackson.databind.DeserializationContext;
import tools.jackson.databind.KeyDeserializer;

import java.util.Objects;

public record ComponentReference<T extends Component>(ComponentType<T> type, ResourceIdentifier id) {

    public String getTranslationKey(TranslationField field) {
        return this.id.getTranslationKey(this.type.name()) + "." + field.key();
    }

    public static <K extends Component> ComponentReference<K> of(ComponentType<K> type, ResourceIdentifier id) {
        return new ComponentReference<>(type, id);
    }

    @JsonCreator
    public static ComponentReference<? extends Component> valueOf(String fqn) {
        String[] split = fqn.split("/");
        if (split.length != 2) {
            throw new IllegalArgumentException("Not a valid component reference string: " + fqn);
        }
        ComponentType<? extends Component> componentType = ComponentTypes.fromName(split[0]);
        if (componentType == null) {
            throw new IllegalArgumentException("Not a valid component type: " + split[0]);
        }

        return of(componentType, ResourceIdentifier.valueOf(split[1]));
    }

    @Override
    public boolean equals(Object o) {
        if (o instanceof ComponentReference<?>(ComponentType<?> thatType, ResourceIdentifier thatId)) {
            return Objects.equals(thatType, this.type) && Objects.equals(thatId, this.id);
        }
        return false;
    }

    @Override
    public String toString() {
        return String.format("%s/%s", this.type.name(), this.id);
    }

    public static class RefKeyDeserializer extends KeyDeserializer {
        @Override
        public Object deserializeKey(String key, DeserializationContext ctxt) {
            return ComponentReference.valueOf(key);
        }
    }
}
