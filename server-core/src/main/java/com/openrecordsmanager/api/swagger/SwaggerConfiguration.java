package com.openrecordsmanager.api.swagger;

import com.openrecordsmanager.api.ApiResponseV1;
import io.swagger.v3.core.converter.ModelConverters;
import io.swagger.v3.oas.models.media.BooleanSchema;
import io.swagger.v3.oas.models.media.Content;
import io.swagger.v3.oas.models.media.MediaType;
import io.swagger.v3.oas.models.media.Schema;
import org.springdoc.core.customizers.GlobalOpenApiCustomizer;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.http.HttpStatus;
import org.springframework.http.HttpStatusCode;

import java.util.Collections;

@Configuration
public class SwaggerConfiguration {

    @Bean
    public GlobalOpenApiCustomizer customerGlobalResponseWrapper() {
        return openApi ->
                openApi.getPaths().values().forEach(pathItem ->
                        pathItem.readOperations().forEach(operation ->
                                operation.getResponses().forEach((stringCode, apiResponse) -> {
                                    HttpStatusCode code;
                                    try {
                                        code = HttpStatus.resolve(Integer.parseInt(stringCode));
                                        if (null == code) return;
                                    } catch (Exception e) {
                                        return;
                                    }

                                    // Dynamically wrap the endpoint's raw schema inside a reusable master metadata template
                                    Content content = apiResponse.getContent();
                                    if (content == null) {
                                        content = new Content();
                                        apiResponse.setContent(content);
                                    }

                                    if (!content.isEmpty() && !content.containsKey("application/json")) {
                                        return;
                                    }

                                    Schema<?> oldSchema = content.containsKey("application/json")
                                            ? content.get("application/json").getSchema()
                                            : null;

                                    Schema<?> schema = ModelConverters.getInstance()
                                            .readAllAsResolvedSchema(ApiResponseV1.class)
                                            .schema;

                                    if (code.isError()) {
                                        Schema<Boolean> falseOnly = new BooleanSchema()
                                                ._enum(Collections.singletonList(Boolean.FALSE));

                                        schema.getProperties().remove("data");
                                        schema.getProperties().put("success", falseOnly);
                                        schema.addRequiredItem("error");
                                    } else {
                                        Schema<Boolean> trueOnly = new BooleanSchema()
                                                ._enum(Collections.singletonList(Boolean.TRUE));

                                        schema.getProperties().remove("error");
                                        schema.getProperties().put("success", trueOnly);

                                        if (oldSchema != null) {
                                            schema.getProperties().put("data", oldSchema);
                                            schema.addRequiredItem("data");
                                        } else {
                                            schema.getProperties().remove("data");
                                        }
                                    }

                                    // Set the schema output to the content
                                    if (content.containsKey("application/json")) {
                                        content.get("application/json").setSchema(schema);
                                    } else {
                                        content.addMediaType("application/json", new MediaType().schema(schema));
                                    }
                                })
                        )
                );
    }
}
