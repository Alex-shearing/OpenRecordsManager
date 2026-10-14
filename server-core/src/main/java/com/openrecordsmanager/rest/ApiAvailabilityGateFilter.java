package com.openrecordsmanager.rest;

import com.openrecordsmanager.api.errors.ApiError;
import com.openrecordsmanager.database.schema.SchemaMigrationState;
import com.openrecordsmanager.rest.dto.ApiResponseV1;
import jakarta.servlet.FilterChain;
import jakarta.servlet.ServletException;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import org.springframework.http.MediaType;
import org.springframework.stereotype.Component;
import org.springframework.web.filter.OncePerRequestFilter;
import tools.jackson.databind.json.JsonMapper;

import java.io.IOException;

/**
 * Blocks business APIs while schema upgrade or first-run setup is required.
 * Registered before token authentication so auth lookups do not hit the database
 * during those states.
 */
@Component
public class ApiAvailabilityGateFilter extends OncePerRequestFilter {

    public static final String UPGRADE_REQUIRED_HEADER = "X-ORM-Schema-Upgrade-Required";

    private final SchemaMigrationState schemaState;
    private final JsonMapper jsonMapper;

    public ApiAvailabilityGateFilter(SchemaMigrationState schemaState, JsonMapper jsonMapper) {
        this.schemaState = schemaState;
        this.jsonMapper = jsonMapper;
    }

    @Override
    protected boolean shouldNotFilter(HttpServletRequest request) {
        String path = request.getRequestURI();
        String contextPath = request.getContextPath();
        if (contextPath != null && !contextPath.isEmpty() && path.startsWith(contextPath)) {
            path = path.substring(contextPath.length());
        }
        return !path.startsWith("/api/")
                || path.startsWith("/api/database/")
                || path.startsWith("/api/setup/")
                || path.startsWith("/api/web")
                || path.startsWith("/api/health")
                || path.startsWith("/api/translations/");
    }

    @Override
    protected void doFilterInternal(
            HttpServletRequest request,
            HttpServletResponse response,
            FilterChain filterChain
    ) throws ServletException, IOException {
        if (this.schemaState.isUpgradeRequired() || this.schemaState.isSetupRequired()) {
            response.setStatus(HttpServletResponse.SC_SERVICE_UNAVAILABLE);
            response.setHeader(UPGRADE_REQUIRED_HEADER, "true");
            response.setContentType(MediaType.APPLICATION_JSON_VALUE);
            this.jsonMapper.writeValue(
                    response.getOutputStream(),
                    ApiResponseV1.error(ApiError.of("schema_upgrade_required"))
            );
            return;
        }

        filterChain.doFilter(request, response);
    }
}
