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
        responseCode = "400",
        description = "Validation failed",
        content = @Content(
                mediaType = "application/json",
                schema = @Schema(ref = API_ERROR_RESPONSE_REF),
                examples = @ExampleObject(
                        value = """
                                {
                                  "success": false,
                                  "error": "validation_failed",
                                  "fieldErrors": {
                                    "field_1": {
                                      "error": "input_schema_validation_failed",
                                      "errorArgs": ["must be a string"]
                                    },
                                    "field_2": {
                                      "error": "input_schema_validation_failed",
                                      "errorArgs": ["is required"]
                                    }
                                  },
                                  "timestamp": "2026-06-29T23:05:00Z"
                                }
                                """
                )
        )
)
public @interface ValidationFailedApiResponse {
}
