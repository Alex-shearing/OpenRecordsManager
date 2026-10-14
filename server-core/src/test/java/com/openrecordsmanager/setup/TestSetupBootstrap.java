package com.openrecordsmanager.setup;

import com.openrecordsmanager.database.schema.SchemaMigrationService;
import com.openrecordsmanager.database.schema.SchemaMigrationState;
import com.openrecordsmanager.setup.dto.CompleteSetupRequest;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.boot.context.event.ApplicationReadyEvent;
import org.springframework.context.event.EventListener;
import org.springframework.core.annotation.Order;
import org.springframework.stereotype.Component;

/**
 * Ensures integration tests get a migrated schema and admin user without production auto-seed.
 */
@Component
@Order(1)
public class TestSetupBootstrap {
    private static final Logger LOGGER = LoggerFactory.getLogger(TestSetupBootstrap.class);

    public static final String TEST_ADMIN_USERNAME = "admin";
    public static final String TEST_ADMIN_PASSWORD = "admin";

    private final SchemaMigrationService schemaMigrationService;
    private final SchemaMigrationState schemaState;
    private final SetupService setupService;

    public TestSetupBootstrap(
            SchemaMigrationService schemaMigrationService,
            SchemaMigrationState schemaState,
            SetupService setupService
    ) {
        this.schemaMigrationService = schemaMigrationService;
        this.schemaState = schemaState;
        this.setupService = setupService;
    }

    @EventListener(ApplicationReadyEvent.class)
    public void bootstrap() {
        if (this.schemaState.isUpgradeRequired()) {
            LOGGER.info("Test bootstrap applying pending schema migrations");
            this.schemaMigrationService.upgrade();
        }
        if (this.setupService.isSetupRequired()) {
            LOGGER.info("Test bootstrap completing first-run setup");
            this.setupService.complete(new CompleteSetupRequest(TEST_ADMIN_USERNAME, TEST_ADMIN_PASSWORD));
        }
    }
}
