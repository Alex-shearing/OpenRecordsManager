package com.openrecordsmanager;

import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;
import org.springframework.boot.persistence.autoconfigure.EntityScan;
import org.springframework.data.jpa.repository.config.EnableJpaRepositories;

@SpringBootApplication
@EnableJpaRepositories(basePackages = "com.openrecordsmanager.model")
@EntityScan(basePackages = "com.openrecordsmanager.model")
public class Main {
    static void main(String[] args) {
        SpringApplication.run(Main.class, args);
    }
}
