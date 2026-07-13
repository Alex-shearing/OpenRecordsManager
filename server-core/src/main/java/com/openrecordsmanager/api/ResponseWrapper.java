package com.openrecordsmanager.api;

import org.jspecify.annotations.Nullable;
import org.springframework.core.MethodParameter;
import org.springframework.core.io.Resource;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
import org.springframework.http.converter.HttpMessageConverter;
import org.springframework.http.server.ServerHttpRequest;
import org.springframework.http.server.ServerHttpResponse;
import org.springframework.web.bind.annotation.RestControllerAdvice;
import org.springframework.web.servlet.mvc.method.annotation.ResponseBodyAdvice;

@RestControllerAdvice
public class ResponseWrapper implements ResponseBodyAdvice<Object> {

    @Override
    public boolean supports(MethodParameter returnType, Class<? extends HttpMessageConverter<?>> converterType) {
        return true;
    }

    @Override
    public @Nullable Object beforeBodyWrite(@Nullable Object body, MethodParameter returnType, MediaType selectedContentType,
                                            Class<? extends HttpMessageConverter<?>> selectedConverterType,
                                            ServerHttpRequest request, ServerHttpResponse response) {

        // If the endpoint already threw an error or explicitly returned an ApiResponse, pass it through
        if (body instanceof ApiResponseV1 || body instanceof ResponseEntity<?> || body instanceof Resource) {
            return body;
        }

        // Do not attempt to transform the swagger endpoints
        String path = request.getURI().getPath();
        if (path.contains("/v3/api-docs") || path.contains("/swagger-ui")) {
            return body;
        }

        return ApiResponseV1.success(body);
    }
}