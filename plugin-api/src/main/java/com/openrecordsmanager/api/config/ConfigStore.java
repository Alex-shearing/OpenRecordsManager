package com.openrecordsmanager.api.config;

import java.util.Set;

public interface ConfigStore {
    <T> T getProperty(ConfigDefinition<T> key);

    Set<ConfigDefinition<?>> getProperties();
}
