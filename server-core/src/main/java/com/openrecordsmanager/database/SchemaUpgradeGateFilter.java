package com.openrecordsmanager.database;

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
 * Blocks business APIs while the schema requires an operator-confirmed upgrade.
 * Registered in the Spring Security filter chain before token authentication so
 * auth lookups do not hit the database while an upgrade is required.
 */
@Component
public class SchemaUpgradeGateFilter extends OncePerRequestFilter {

    public static final String UPGRADE_REQUIRED_HEADER = "X-ORM-Schema-Upgrade-Required";

    private final SchemaMigrationState state;
    private final JsonMapper jsonMapper;

    public SchemaUpgradeGateFilter(SchemaMigrationState state, JsonMapper jsonMapper) {
        this.state = state;
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
                || path.startsWith("/api/web")
                || path.startsWith("/api/health");
    }

    @Override
    protected void doFilterInternal(
            HttpServletRequest request,
            HttpServletResponse response,
            FilterChain filterChain
    ) throws ServletException, IOException {
        if (!this.state.isUpgradeRequired()) {
            filterChain.doFilter(request, response);
            return;
        }

        response.setStatus(HttpServletResponse.SC_SERVICE_UNAVAILABLE);
        response.setHeader(UPGRADE_REQUIRED_HEADER, "true");
        response.setContentType(MediaType.APPLICATION_JSON_VALUE);

        String message = this.state.getMessage();
        this.jsonMapper.writeValue(
                response.getOutputStream(),
                ApiResponseV1.error(
                        message != null ? message : "Database schema upgrade required"
                )
        );
    }
}
