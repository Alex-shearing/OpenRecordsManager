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
        responseCode = "500",
        description = "Internal Server error",
        content = @Content(
                mediaType = "application/json",
                schema = @Schema(ref = API_ERROR_RESPONSE_REF),
                examples = @ExampleObject(
                        value = """
                                {
                                  "success": false,
                                  "error": "internal_server_error",
                                  "timestamp": "2026-06-29T23:05:00Z"
                                }
                                """
                )
        )
)
public @interface InternalServerErrorApiResponse {
}
