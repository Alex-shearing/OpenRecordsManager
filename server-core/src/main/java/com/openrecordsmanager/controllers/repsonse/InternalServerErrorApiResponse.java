package com.openrecordsmanager.controllers.repsonse;

import com.openrecordsmanager.controllers.repsonse.errors.ApiResponseWrapper;
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
                schema = @Schema(implementation = ApiResponseWrapper.class),
                examples = @ExampleObject(
                        value = """
                                {
                                  "success": false,
                                  "errorCode": "Internal Server Error",
                                  "timestamp": "2026-06-29T23:05:00Z"
                                }
                                """
                )
        )
)
public @interface InternalServerErrorApiResponse {
}
