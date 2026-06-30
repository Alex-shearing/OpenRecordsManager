package com.openrecordsmanager;

import io.swagger.v3.oas.models.OpenAPI;
import io.swagger.v3.oas.models.info.Info;
import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;
import org.springframework.boot.persistence.autoconfigure.EntityScan;
import org.springframework.context.ApplicationContext;
import org.springframework.context.annotation.Bean;
import org.springframework.data.jpa.repository.config.EnableJpaRepositories;
import org.springframework.http.support.JacksonHandlerInstantiator;
import tools.jackson.databind.json.JsonMapper;

@SpringBootApplication
@EnableJpaRepositories(basePackages = "com.openrecordsmanager.model")
@EntityScan(basePackages = "com.openrecordsmanager.model")
public class Main {
    static void main(String[] args) {
        SpringApplication.run(Main.class, args);
    }

    @Bean
    public JsonMapper jsonMapper(ApplicationContext applicationContext) {
        JacksonHandlerInstantiator instantiator = new JacksonHandlerInstantiator(applicationContext.getAutowireCapableBeanFactory());
        return JsonMapper.builder().handlerInstantiator(instantiator).build();
    }

    @Bean
    public OpenAPI customOpenAPI() {
        return new OpenAPI()
                .info(new Info()
                        .title("Open Record Manager API")
                        .version("1.0.0")
                        .description("REST API for the Open Records Management system")
                );
    }
}
