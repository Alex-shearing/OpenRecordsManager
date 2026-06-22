package com.openrecordsmanager.api;

import com.openrecordsmanager.api.property.PropertyDefinition;

public interface RecordObject {
    <T> T getProperty(PropertyDefinition<T> property);
}
