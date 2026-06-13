package com.openrecordsmanager;

import com.openrecordsmanager.config.ConfigStoreImpl;
import org.jspecify.annotations.NonNull;
import org.springframework.boot.EnvironmentPostProcessor;
import org.springframework.boot.SpringApplication;
import org.springframework.core.env.ConfigurableEnvironment;

public class CustomConfig implements EnvironmentPostProcessor {
    @Override
    public void postProcessEnvironment(ConfigurableEnvironment environment, @NonNull SpringApplication application) {
        ConfigStoreImpl conf = ConfigStoreImpl.build(null);
        environment.getPropertySources().addFirst(conf);
    }
}
