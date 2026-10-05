package com.openrecordsmanager.rest.swagger;

import com.openrecordsmanager.auth.AuthService;
import com.openrecordsmanager.rest.dto.ApiResponseV1;
import io.swagger.v3.core.converter.ModelConverter;
import io.swagger.v3.core.jackson.ModelResolver;
import io.swagger.v3.oas.models.Components;
import io.swagger.v3.oas.models.OpenAPI;
import io.swagger.v3.oas.models.Operation;
import io.swagger.v3.oas.models.info.Info;
import io.swagger.v3.oas.models.media.*;
import io.swagger.v3.oas.models.security.SecurityRequirement;
import io.swagger.v3.oas.models.security.SecurityScheme;
import org.springdoc.core.customizers.OpenApiCustomizer;
import org.springdoc.core.customizers.OperationCustomizer;
import org.springdoc.core.utils.SpringDocUtils;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.http.HttpHeaders;
import org.springframework.http.HttpStatus;
import org.springframework.http.HttpStatusCode;
import org.springframework.http.ResponseEntity;
import org.springframework.web.method.HandlerMethod;
import tools.jackson.databind.JsonNode;

import java.util.List;

@Configuration
public class SwaggerConfiguration {

    public static final String API_ERROR_RESPONSE = "ApiErrorResponse";
    public static final String API_SUCCESS_ENVELOPE = "ApiSuccessEnvelope";
    public static final String API_FIELD_ERROR = "ApiFieldError";
    
    public static final String API_ERROR_RESPONSE_REF = "#/components/schemas/" + API_ERROR_RESPONSE;

    private static final String REF_SUCCESS = "#/components/schemas/" + API_SUCCESS_ENVELOPE;
    private static final String REF_FIELD_ERROR = "#/components/schemas/" + API_FIELD_ERROR;

    static {
        // Prefer free-form JSON over JsonNode bean introspection (also covers early schema caching).
        SpringDocUtils.getConfig().replaceWithSchema(JsonNode.class, JsonNodeModelConverter.freeFormJsonSchema());
        // Emit enums as named component schemas so openapi-ts generates reusable JS enums.
        ModelResolver.enumsAsRef = true;
    }

    @Bean
    public ModelConverter jsonNodeModelConverter() {
        return new JsonNodeModelConverter();
    }

    /**
     * Fix discriminated unions on {@link com.openrecordsmanager.config.dto.ConfigTypeResponse}
     */
    @Bean
    public OpenApiCustomizer configTypeResponseOpenApiCustomizer() {
        return openApi -> {
            if (openApi.getComponents() != null && openApi.getComponents().getSchemas() != null) {
                Schema<?> parentSchema = openApi.getComponents().getSchemas().get("ConfigTypeResponse");

                if (parentSchema != null && parentSchema.getOneOf() != null) {
                    parentSchema.setProperties(null);
                    parentSchema.setType(null);
                }
            }
        };
    }

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
                    mediaType.setSchema(new Schema<>().$ref(API_ERROR_RESPONSE_REF));
                    return;
                }

                Schema<?> dataProperty = mediaType.getSchema();

                if (dataProperty == null) {
                    mediaType.setSchema(new Schema<>().$ref(REF_SUCCESS));
                    return;
                }

                mediaType.setSchema(new Schema<>()
                        .allOf(List.of(
                                new Schema<>().$ref(REF_SUCCESS),
                                new ObjectSchema()
                                        .addProperty("data", dataProperty)
                                        .addRequiredItem("data")
                        ))
                );
            });

            return operation;
        };
    }

    static Schema<?> fieldErrorSchema() {
        return new ObjectSchema()
                .description("Field-level validation error")
                .addProperty("error", new StringSchema()).addRequiredItem("error")
                .addProperty("errorArgs", new ArraySchema().items(new StringSchema()));
    }

    static Schema<?> errorSchema() {
        return new ObjectSchema()
                .description("Failed API envelope")
                .addProperty("success", new BooleanSchema()._const(false)).addRequiredItem("success")
                .addProperty("timestamp", new StringSchema().format("date-time")).addRequiredItem("timestamp")
                .addProperty("error", new StringSchema()).addRequiredItem("error")
                .addProperty("errorArgs", new ArraySchema().items(new StringSchema()))
                .addProperty("fieldErrors", new ObjectSchema().additionalProperties(new Schema<>().$ref(REF_FIELD_ERROR)));
    }

    static Schema<?> successEnvelopeSchema() {
        return new ObjectSchema()
                .description("Successful API envelope without payload")
                .addProperty("success", new BooleanSchema()._const(true)).addRequiredItem("success")
                .addProperty("timestamp", new StringSchema().format("date-time")).addRequiredItem("timestamp");
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
                        .addSchemas(API_FIELD_ERROR, fieldErrorSchema())
                        .addSchemas(API_ERROR_RESPONSE, errorSchema())
                        .addSchemas(API_SUCCESS_ENVELOPE, successEnvelopeSchema())
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
