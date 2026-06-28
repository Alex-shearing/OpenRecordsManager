package com.openrecordsmanager.config;

import com.google.common.collect.ImmutableSet;
import com.openrecordsmanager.api.config.ConfigDefinition;
import com.openrecordsmanager.api.config.ConfigStore;
import com.openrecordsmanager.model.repositories.DataRepository;
import com.openrecordsmanager.resources.ComponentCatalog;
import com.openrecordsmanager.resources.types.ComponentTypes;
import jakarta.annotation.Nullable;
import org.jspecify.annotations.NonNull;
import org.jspecify.annotations.NullMarked;
import org.springframework.core.env.EnumerablePropertySource;

import java.util.Arrays;
import java.util.Objects;
import java.util.Optional;
import java.util.Set;
import java.util.stream.Collectors;
import java.util.stream.Stream;

public class DatabaseConfigSource extends EnumerablePropertySource<DatabaseConfigSource> implements ConfigStore {
    private Set<ConfigDefinition<?>> configs;
    @Nullable
    private DataRepository repository;

    public DatabaseConfigSource() {
        super("custom_config");
        this.configs = Arrays.stream(ConfigProperties.BUILTIN_CONFIG).collect(Collectors.toSet());
    }

    public <T> T getProperty(ConfigDefinition<T> key) {
        if (this.repository != null) {
            Optional<T> val = this.repository.configRepo.findByConfigKey(key.id())
                    .flatMap(dbConfig -> key.type().fromString(dbConfig.configValue));
            
            if (val.isPresent()) {
                return val.get();
            }
        }
        return key.defaultValue();
    }

    @Override
    public Set<ConfigDefinition<?>> getProperties() {
        return this.configs;
    }

    @Override
    @NullMarked
    public String[] getPropertyNames() {
        return this.configs.stream()
                .flatMap(def -> Stream.of(def.id(), def.alias()))
                .filter(Objects::nonNull)
                .toArray(String[]::new);
    }

    @Override
    public @Nullable Object getProperty(@NonNull String name) {
        return this.configs.stream()
                .filter(configProperty -> configProperty.isKey(name))
                .map(this::getProperty)
                .filter(Objects::nonNull)
                .findFirst()
                .orElse(null);
    }

    public void initialiseLoader(ComponentCatalog catalog, DataRepository repository) {
        this.repository = repository;

        ImmutableSet.Builder<ConfigDefinition<?>> propertiesBuilder = ImmutableSet.builder();

        // Load defaults into map
        for (ConfigDefinition<?> property : ConfigProperties.BUILTIN_CONFIG) {
            propertiesBuilder.add(property);
        }
        if (catalog != null) {
            for (ConfigDefinition<?> property : catalog.getComponents(ComponentTypes.CONFIG)) {
                propertiesBuilder.add(property);
            }
        }

        this.configs = propertiesBuilder.build();
    }
}
