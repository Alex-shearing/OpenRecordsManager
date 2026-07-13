package com.openrecordsmanager.api.swagger;

import com.openrecordsmanager.api.ApiResponseV1;
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
        responseCode = "409",
        description = "Resource in use",
        content = @Content(
                mediaType = "application/json",
                schema = @Schema(implementation = ApiResponseV1.class),
                examples = @ExampleObject(
                        value = """
                                {
                                  "success": false,
                                  "error": "Cannot delete a store that contains files",
                                  "timestamp": "2026-06-29T23:05:00Z"
                                }
                                """
                )
        )
)
@ResponseStatus(HttpStatus.CONFLICT)
public @interface ConflictApiResponse {
}
