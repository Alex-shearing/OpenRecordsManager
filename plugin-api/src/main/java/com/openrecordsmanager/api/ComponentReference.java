package com.openrecordsmanager.api;

import com.openrecordsmanager.api.types.ComponentType;

public abstract class ComponentReference<T extends Component> {

    abstract public T getComponent(IComponentCatalog catalog);

    public static class Reference<T extends Component> extends ComponentReference<T> {
        private final ComponentType<T> type;
        private final ResourceIdentifier id;

        public Reference(ComponentType<T> type, ResourceIdentifier id) {
            this.type = type;
            this.id = id;
        }

        @Override
        public T getComponent(IComponentCatalog catalog) {
            return catalog.getComponent(this.type, this.id);
        }
    }

    public static class Value<T extends Component> extends ComponentReference<T> {
        private final T value;

        public Value(T value) {
            this.value = value;
        }

        @Override
        public T getComponent(IComponentCatalog catalog) {
            return this.value;
        }
    }
}
