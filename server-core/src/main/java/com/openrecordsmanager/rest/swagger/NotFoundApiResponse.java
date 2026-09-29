package com.openrecordsmanager.rest.swagger;

import static com.openrecordsmanager.rest.swagger.SwaggerConfiguration.API_ERROR_RESPONSE_REF;
import io.swagger.v3.oas.annotations.media.Content;
import io.swagger.v3.oas.annotations.media.ExampleObject;
import io.swagger.v3.oas.annotations.media.Schema;
import io.swagger.v3.oas.annotations.responses.ApiResponse;

import java.lang.annotation.*;

@Target({ElementType.METHOD, ElementType.TYPE})
@Retention(RetentionPolicy.RUNTIME)
@Inherited
@ApiResponse(
        responseCode = "404",
        description = "Not Found",
        content = @Content(
                mediaType = "application/json",
                schema = @Schema(ref = API_ERROR_RESPONSE_REF),
                examples = @ExampleObject(
                        value = """
                                {
                                  "success": false,
                                  "error": "resource_not_found",
                                  "errorArgs": ["550e8400-e29b-41d4-a716-446655440000", "user"],
                                  "timestamp": "2026-06-29T23:05:00Z"
                                }
                                """
                )
        )
)
public @interface NotFoundApiResponse {
}
