package com.openrecordsmanager.rest.swagger;

import io.swagger.v3.oas.annotations.responses.ApiResponse;

import java.lang.annotation.*;

@Target({ElementType.METHOD, ElementType.TYPE})
@Retention(RetentionPolicy.RUNTIME)
@Inherited
@InternalServerErrorApiResponse
@ForbiddenApiResponse
@UnauthorizedApiResponse
@ApiResponse(responseCode = "200")
public @interface DefaultApiResponses {
}
