package com.openrecordsmanager.api.swagger;

import com.openrecordsmanager.api.ApiResponseV1;
import com.openrecordsmanager.auth.AuthService;
import io.swagger.v3.oas.models.Components;
import io.swagger.v3.oas.models.OpenAPI;
import io.swagger.v3.oas.models.Operation;
import io.swagger.v3.oas.models.info.Info;
import io.swagger.v3.oas.models.media.*;
import io.swagger.v3.oas.models.security.SecurityRequirement;
import io.swagger.v3.oas.models.security.SecurityScheme;
import org.springdoc.core.customizers.OperationCustomizer;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.http.HttpHeaders;
import org.springframework.http.HttpStatus;
import org.springframework.http.HttpStatusCode;
import org.springframework.http.ResponseEntity;
import org.springframework.web.method.HandlerMethod;

@Configuration
public class SwaggerConfiguration {

    @Bean
    public OperationCustomizer wrapResponseSchemaCustomizer() {
        return (Operation operation, HandlerMethod handlerMethod) -> {
            if (operation.getResponses() == null) {
                return operation;
            }

            operation.getResponses().forEach((stringCode, apiResponse) -> {
                HttpStatusCode code;
                try {
                    code = HttpStatus.resolve(Integer.parseInt(stringCode));
                    if (null == code) return;
                } catch (Exception e) {
                    return;
                }

                Class<?> originalRt = handlerMethod.getReturnType().getParameterType();
                if (originalRt == ApiResponseV1.class || originalRt == ResponseEntity.class) {
                    return;
                }

                Content content = apiResponse.getContent();
                if (content == null) {
                    content = new Content();
                    apiResponse.setContent(content);
                }

                MediaType mediaType = content.computeIfAbsent("application/json", k -> new MediaType());

                if (code.isError()) {
                    mediaType.setSchema(getErrorSchema());
                    return;
                }

                Schema<?> wrappedSchema = new Schema<>()
                        .type("object")
                        .name("Response")
                        .addProperty("success", new BooleanSchema()._const(true)).addRequiredItem("success")
                        .addProperty("timestamp", new Schema<>().type("string").format("date-time")).addRequiredItem("timestamp");

                Schema<?> originalSchema = mediaType.getSchema();
                if (originalSchema != null) {
                    wrappedSchema
                            .name("Wrapped_" + originalSchema.getType())
                            .addProperty("data", originalSchema).addRequiredItem("data");
                }

                // Override the old flat payload mapping with our custom generic layout
                mediaType.setSchema(wrappedSchema);
            });

            return operation;
        };
    }

    private static Schema<?> getErrorSchema() {
        return new Schema<>()
                .type("object")
                .name("OrmError")
                .addProperty("success", new BooleanSchema()._const(false)).addRequiredItem("success")
                .addProperty("timestamp", new Schema<>().type("string").format("date-time")).addRequiredItem("timestamp")
                .addProperty("error", new StringSchema()).addRequiredItem("error");
    }

    @Bean
    public OpenAPI customOpenAPI(AuthService authService) {
        return new OpenAPI()
                .info(new Info()
                        .title("Open Record Manager API")
                        .version("1.0.0")
                        .description("REST API for the Open Records Management system")
                )
                .components(new Components()
                        .addSecuritySchemes("bearerAuth", new SecurityScheme()
                                .type(SecurityScheme.Type.HTTP)
                                .in(SecurityScheme.In.HEADER)
                                .scheme("bearer")
                                .name(HttpHeaders.AUTHORIZATION)
                                .description("Should be used for integrations, use the /api/auth/login endpoint.")
                        )
                        .addSecuritySchemes("cookieAuth", new SecurityScheme()
                                .type(SecurityScheme.Type.APIKEY)
                                .in(SecurityScheme.In.COOKIE)
                                .name(authService.getCookieName())
                                .description("Used by the web client, this authentication method is CSRF protected.")
                        )
                )
                .addSecurityItem(new SecurityRequirement().addList("bearerAuth").addList("cookieAuth"));
    }
}
