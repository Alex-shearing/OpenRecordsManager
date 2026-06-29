package com.openrecordsmanager.controllers.repsonse;

import io.swagger.v3.oas.models.media.Content;
import io.swagger.v3.oas.models.media.Schema;
import org.springdoc.core.customizers.GlobalOpenApiCustomizer;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

@Configuration
public class SwaggerConfiguration {

    @Bean
    public GlobalOpenApiCustomizer customerGlobalResponseWrapper() {
        return openApi -> {
            openApi.getPaths().values().forEach(pathItem -> {
                pathItem.readOperations().forEach(operation -> {
                    operation.getResponses().values().forEach(apiResponse -> {
                        Content content = apiResponse.getContent();
                        if (content != null && content.containsKey("application/json")) {
                            // Dynamically wrap the endpoint's raw schema inside a reusable master metadata template
                            Schema<?> wrappedSchema = new Schema<>()
                                    .addProperty("success", new Schema<>().type("boolean").example(true))
                                    .addProperty("timestamp", new Schema<>().type("string").format("date-time"))
                                    .addProperty("data", content.get("application/json").getSchema());

                            content.get("application/json").setSchema(wrappedSchema);
                        }
                    });
                });
            });
        };
    }
}
