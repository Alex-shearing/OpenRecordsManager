package com.openrecordsmanager;

import com.openrecordsmanager.auth.AuthServices;
import io.swagger.v3.oas.models.Components;
import io.swagger.v3.oas.models.OpenAPI;
import io.swagger.v3.oas.models.info.Info;
import io.swagger.v3.oas.models.security.SecurityRequirement;
import io.swagger.v3.oas.models.security.SecurityScheme;
import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;
import org.springframework.boot.persistence.autoconfigure.EntityScan;
import org.springframework.context.ApplicationContext;
import org.springframework.context.annotation.Bean;
import org.springframework.data.jpa.repository.config.EnableJpaRepositories;
import org.springframework.http.support.JacksonHandlerInstantiator;
import tools.jackson.databind.json.JsonMapper;

import java.util.List;

@SpringBootApplication
@EnableJpaRepositories(basePackages = "com.openrecordsmanager")
@EntityScan(basePackages = "com.openrecordsmanager")
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
    public OpenAPI customOpenAPI(AuthServices authService) {
        return new OpenAPI()
                .info(new Info()
                        .title("Open Record Manager API")
                        .version("1.0.0")
                        .description("REST API for the Open Records Management system")
                )
                .components(new Components()
                        .addSecuritySchemes("bearerAuth", new SecurityScheme()
                                .type(SecurityScheme.Type.HTTP)
                                .scheme("bearer")
                                .bearerFormat("JWT")
                                .in(SecurityScheme.In.HEADER)
                                .name("Authorization"))
                        .addSecuritySchemes("cookieAuth", new SecurityScheme()
                                .type(SecurityScheme.Type.APIKEY)
                                .in(SecurityScheme.In.COOKIE)
                                .name(authService.getCookieName())))
                .security(List.of(
                        new SecurityRequirement().addList("bearerAuth"),
                        new SecurityRequirement().addList("cookieAuth")
                ));
    }
}
