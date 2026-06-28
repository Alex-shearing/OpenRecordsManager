package com.openrecordsmanager.config;

import com.openrecordsmanager.model.repositories.DataRepository;
import com.openrecordsmanager.resources.ComponentCatalog;
import org.jspecify.annotations.NonNull;
import org.springframework.boot.ApplicationArguments;
import org.springframework.boot.ApplicationRunner;
import org.springframework.core.env.ConfigurableEnvironment;
import org.springframework.core.env.PropertySource;
import org.springframework.stereotype.Component;

@Component
public class DatabaseConfigInitializationTask implements ApplicationRunner {

    private final ConfigurableEnvironment environment;
    private final DataRepository repository;
    private final ComponentCatalog catalog;

    public DatabaseConfigInitializationTask(ConfigurableEnvironment environment, DataRepository repository, ComponentCatalog catalog) {
        this.environment = environment;
        this.repository = repository;
        this.catalog = catalog;
    }

    @Override
    public void run(@NonNull ApplicationArguments args) {
        for (PropertySource<?> propertySource : this.environment.getPropertySources()) {
            if (propertySource instanceof DatabaseConfigSource dbCfgSrc) {
                dbCfgSrc.initialiseLoader(this.catalog, this.repository);
            }
        }
    }
}
