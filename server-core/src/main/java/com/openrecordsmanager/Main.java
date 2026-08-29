package com.openrecordsmanager;

import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;
import org.springframework.boot.persistence.autoconfigure.EntityScan;
import org.springframework.context.ApplicationContext;
import org.springframework.context.annotation.Bean;
import org.springframework.data.jpa.repository.config.EnableJpaRepositories;
import org.springframework.http.support.JacksonHandlerInstantiator;
import org.springframework.scheduling.annotation.EnableScheduling;
import tools.jackson.databind.json.JsonMapper;

@SpringBootApplication
@EnableJpaRepositories(basePackages = "com.openrecordsmanager")
@EntityScan(basePackages = "com.openrecordsmanager")
@EnableScheduling
public class Main {
    static void main(String[] args) {
        SpringApplication.run(Main.class, args);
    }

    @Bean
    public JsonMapper jsonMapper(ApplicationContext applicationContext) {
        JacksonHandlerInstantiator instantiator = new JacksonHandlerInstantiator(applicationContext.getAutowireCapableBeanFactory());
        return JsonMapper.builder().handlerInstantiator(instantiator).build();
    }

}
