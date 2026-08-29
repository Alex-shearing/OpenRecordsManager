package com.openrecordsmanager.rest.swagger;

import com.openrecordsmanager.rest.dto.ApiResponseV1;
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
                schema = @Schema(implementation = ApiResponseV1.class),
                examples = @ExampleObject(
                        value = """
                                {
                                  "success": false,
                                  "error": "Internal Server Error",
                                  "timestamp": "2026-06-29T23:05:00Z"
                                }
                                """
                )
        )
)
public @interface InternalServerErrorApiResponse {
}
