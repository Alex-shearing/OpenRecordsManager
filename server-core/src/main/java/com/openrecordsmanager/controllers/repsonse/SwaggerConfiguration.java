package com.openrecordsmanager.controllers.repsonse;

import com.openrecordsmanager.controllers.repsonse.errors.ApiResponseWrapper;
import io.swagger.v3.core.converter.ModelConverters;
import io.swagger.v3.oas.models.media.Content;
import io.swagger.v3.oas.models.media.Schema;
import org.springdoc.core.customizers.GlobalOpenApiCustomizer;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

@Configuration
public class SwaggerConfiguration {

    @Bean
    public GlobalOpenApiCustomizer customerGlobalResponseWrapper() {
        return openApi ->
                openApi.getPaths().values().forEach(pathItem ->
                        pathItem.readOperations().forEach(operation ->
                                operation.getResponses().forEach((_, apiResponse) -> {
                                    Content content = apiResponse.getContent();
                                    if (content != null && content.containsKey("application/json")) {
                                        // Dynamically wrap the endpoint's raw schema inside a reusable master metadata template
                                        Schema<?> schema = ModelConverters.getInstance()
                                                .readAllAsResolvedSchema(ApiResponseWrapper.class)
                                                .schema;
                                        schema.getProperties().put("data", content.get("application/json").getSchema());

                                        content.get("application/json").setSchema(schema);
                                    }
                                })
                        )
                );
    }
}
