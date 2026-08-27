package com.openrecordsmanager.database;

import com.openrecordsmanager.database.dto.SchemaValidationResponse;
import com.openrecordsmanager.database.dto.SetupStatusResponse;
import com.openrecordsmanager.database.schema.SchemaMigrationService;
import com.openrecordsmanager.database.schema.SchemaValidationService;
import com.openrecordsmanager.rest.swagger.InternalServerErrorApiResponse;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.responses.ApiResponse;
import org.springframework.http.MediaType;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequestMapping("/api/database")
@InternalServerErrorApiResponse
@ApiResponse(responseCode = "200")
public class DatabaseController {

    private final SchemaMigrationService schemaMigrationService;
    private final SchemaValidationService schemaValidationService;

    public DatabaseController(SchemaMigrationService schemaMigrationService, SchemaValidationService schemaValidationService) {
        this.schemaMigrationService = schemaMigrationService;
        this.schemaValidationService = schemaValidationService;
    }

    @GetMapping(value = "/status", produces = MediaType.APPLICATION_JSON_VALUE)
    @Operation(summary = "Get database schema migration status")
    public SetupStatusResponse status() {
        return this.schemaMigrationService.toStatusResponse(null);
    }

    @PostMapping(value = "/upgrade", produces = MediaType.APPLICATION_JSON_VALUE)
    @Operation(summary = "Apply pending database schema migrations")
    public SetupStatusResponse upgrade() {
        return this.schemaMigrationService.upgrade();
    }

    @PostMapping(value = "/validate", produces = MediaType.APPLICATION_JSON_VALUE)
    @Operation(summary = "Validate the database matches the expected schema")
    public SchemaValidationResponse validate() {
        return this.schemaValidationService.validate();
    }
}
