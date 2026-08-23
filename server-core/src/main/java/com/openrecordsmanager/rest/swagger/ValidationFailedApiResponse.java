package com.openrecordsmanager.rest.swagger;

import com.openrecordsmanager.rest.ApiResponseV1;
import io.swagger.v3.oas.annotations.media.Content;
import io.swagger.v3.oas.annotations.media.ExampleObject;
import io.swagger.v3.oas.annotations.media.Schema;
import io.swagger.v3.oas.annotations.responses.ApiResponse;
import org.springframework.http.HttpStatus;
import org.springframework.web.bind.annotation.ResponseStatus;

import java.lang.annotation.*;

@Target({ElementType.METHOD, ElementType.TYPE})
@Retention(RetentionPolicy.RUNTIME)
@Inherited
@ApiResponse(
        responseCode = "400",
        description = "Validation failed",
        content = @Content(
                mediaType = "application/json",
                schema = @Schema(implementation = ApiResponseV1.class),
                examples = @ExampleObject(
                        value = """
                                {
                                  "success": false,
                                  "error": "Validation failed",
                                  "error_data": {
                                    "field_1": "error details",
                                    "field_2": "error details"
                                  },
                                  "timestamp": "2026-06-29T23:05:00Z"
                                }
                                """
                )
        )
)
@ResponseStatus(HttpStatus.BAD_REQUEST)
public @interface ValidationFailedApiResponse {
}
