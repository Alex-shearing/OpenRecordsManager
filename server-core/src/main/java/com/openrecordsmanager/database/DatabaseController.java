package com.openrecordsmanager.database;

import com.openrecordsmanager.database.dto.DatabaseStatusResponse;
import com.openrecordsmanager.database.schema.SchemaMigrationService;
import com.openrecordsmanager.database.schema.SchemaMigrationState;
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
    private final SchemaMigrationState schemaState;
    private final DatabaseWritableProbe probe;

    public DatabaseController(
            SchemaMigrationService schemaMigrationService,
            SchemaValidationService schemaValidationService,
            SchemaMigrationState schemaState,
            DatabaseWritableProbe probe
    ) {
        this.schemaMigrationService = schemaMigrationService;
        this.schemaValidationService = schemaValidationService;
        this.schemaState = schemaState;
        this.probe = probe;
    }

    @GetMapping(value = "/status", produces = MediaType.APPLICATION_JSON_VALUE)
    @Operation(summary = "Get database schema migration and first-run setup status")
    public DatabaseStatusResponse getSchemaMigrationStatus() {
        return this.toStatus(-1);
    }

    @PostMapping(value = "/upgrade", produces = MediaType.APPLICATION_JSON_VALUE)
    @Operation(summary = "Apply pending database schema migrations")
    public DatabaseStatusResponse upgradeSchema() {
        int migrationsApplied = this.schemaMigrationService.upgrade();
        return this.toStatus(migrationsApplied);
    }

    @PostMapping(value = "/validate", produces = MediaType.APPLICATION_JSON_VALUE)
    @Operation(summary = "Validate the database matches the expected schema")
    public boolean validateSchema() {
        return this.schemaValidationService.validate();
    }

    private DatabaseStatusResponse toStatus(int migrationsApplied) {
        this.probe.probe();
        return this.schemaMigrationService.toStatusResponse(
                migrationsApplied,
                !this.schemaState.isUpgradeRequired() && this.probe.isWritable()
        );
    }
}
