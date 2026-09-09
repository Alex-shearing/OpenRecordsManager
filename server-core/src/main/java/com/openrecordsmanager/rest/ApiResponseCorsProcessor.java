package com.openrecordsmanager.rest;

import com.openrecordsmanager.rest.dto.ApiResponseV1;
import org.springframework.http.HttpStatus;
import org.springframework.http.MediaType;
import org.springframework.http.server.ServerHttpResponse;
import org.springframework.web.cors.DefaultCorsProcessor;
import tools.jackson.databind.json.JsonMapper;

import java.io.IOException;

/**
 * Writes CORS rejections as {@link ApiResponseV1} JSON so clients get the same envelope
 * as {@code ExceptionController} errors, instead of Spring's bare {@code Invalid CORS request} text.
 */
public class ApiResponseCorsProcessor extends DefaultCorsProcessor {

    private final JsonMapper jsonMapper;

    public ApiResponseCorsProcessor(JsonMapper jsonMapper) {
        this.jsonMapper = jsonMapper;
    }

    @Override
    protected void rejectRequest(ServerHttpResponse response) throws IOException {
        response.setStatusCode(HttpStatus.FORBIDDEN);
        response.getHeaders().setContentType(MediaType.APPLICATION_JSON);
        this.jsonMapper.writeValue(response.getBody(), ApiResponseV1.error("Invalid CORS request"));
        response.flush();
    }
}
