package com.openrecordsmanager;

import com.openrecordsmanager.property.PropertyDefinition;

public interface RecordObject {
    <T> T getProperty(PropertyDefinition<T> property);
}
