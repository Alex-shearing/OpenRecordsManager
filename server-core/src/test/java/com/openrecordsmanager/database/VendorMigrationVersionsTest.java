package com.openrecordsmanager.database;

import org.junit.jupiter.api.Test;

import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.stream.Collectors;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;

class VendorMigrationVersionsTest {

    private static final List<String> VENDORS = List.of("sqlite", "sqlserver", "postgresql", "mariadb");

    @Test
    void allVendorsHaveMatchingMigrationVersions() throws IOException {
        Path migrationRoot = Path.of("src/main/resources/db/migration");
        Map<String, Set<String>> versionsByVendor = VENDORS.stream()
                .collect(Collectors.toMap(
                        vendor -> vendor,
                        vendor -> migrationVersions(migrationRoot.resolve(vendor))
                ));

        Set<String> referenceVersions = versionsByVendor.get(VENDORS.getFirst());
        assertFalse(referenceVersions.isEmpty(), "Expected at least one migration script per vendor");

        for (String vendor : VENDORS) {
            assertEquals(
                    referenceVersions,
                    versionsByVendor.get(vendor),
                    () -> "Migration version mismatch for vendor '" + vendor + "'. "
                            + "Each vendor folder under db/migration must contain the same Flyway versions."
            );
        }
    }

    private static Set<String> migrationVersions(Path vendorDirectory) {
        try (var paths = Files.list(vendorDirectory)) {
            return paths
                    .map(path -> path.getFileName().toString())
                    .filter(name -> name.matches("V\\d+__.*\\.sql"))
                    .map(name -> name.substring(0, name.indexOf("__")))
                    .collect(Collectors.toUnmodifiableSet());
        } catch (IOException e) {
            throw new IllegalStateException("Unable to read migrations from " + vendorDirectory, e);
        }
    }
}
