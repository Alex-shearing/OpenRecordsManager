package com.openrecordsmanager;

import com.openrecordsmanager.api.config.ConfigStore;
import com.openrecordsmanager.config.ConfigStoreImpl;
import com.openrecordsmanager.resources.ResourceCatalog;
import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;
import org.springframework.boot.persistence.autoconfigure.EntityScan;
import org.springframework.context.annotation.Bean;
import org.springframework.data.jpa.repository.config.EnableJpaRepositories;

@SpringBootApplication
@EnableJpaRepositories(basePackages = "com.openrecordsmanager.model")
@EntityScan(basePackages = "com.openrecordsmanager.model")
public class Main {
    static void main(String[] args) {
        SpringApplication.run(Main.class, args);
    }

    @Bean
    public ConfigStore configStore(ResourceCatalog manager) {
        return ConfigStoreImpl.build(manager);
    }
}
