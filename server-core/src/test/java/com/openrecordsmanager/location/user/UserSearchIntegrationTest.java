package com.openrecordsmanager.location.user;

import com.openrecordsmanager.api.builtin.BuiltinConfigs;
import com.openrecordsmanager.api.ResourceIdentifier;
import com.openrecordsmanager.api.builtin.BuiltinPlugin;
import com.openrecordsmanager.api.builtin.BuiltinPropertyIds;
import com.openrecordsmanager.api.location.LocationKind;
import com.openrecordsmanager.api.search.SearchClause;
import com.openrecordsmanager.api.search.SearchOperator;
import com.openrecordsmanager.database.DataRepository;
import com.openrecordsmanager.database.SqliteTestSupport;
import com.openrecordsmanager.location.LocationService;
import com.openrecordsmanager.location.dto.LocationSearchResponse;
import com.openrecordsmanager.search.dto.ObjectSearchRequest;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.test.context.DynamicPropertyRegistry;
import org.springframework.test.context.DynamicPropertySource;

import java.util.List;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertTrue;

@SpringBootTest
class UserSearchIntegrationTest {

    @DynamicPropertySource
    static void properties(DynamicPropertyRegistry registry) {
        SqliteTestSupport.registerPrimaryMemoryDatabase(registry, UserSearchIntegrationTest.class);
        registry.add(BuiltinConfigs.PLUGINS_SKIP_SYNC.key(), () -> "true");
        registry.add(BuiltinConfigs.COOKIE_SECURE.key(), () -> "false");
    }

    @Autowired
    private LocationService locationService;

    @Autowired
    private DataRepository repository;

    @Test
    void searchByNameDefaultQ() {
        User admin = this.repository.userRepo.findByName("admin").orElseThrow();

        LocationSearchResponse response = this.locationService.search(
                admin,
                LocationKind.USER,
                new ObjectSearchRequest("admin", null, null, null, null, null)
        );

        assertEquals(1, response.items().size());
        assertEquals("admin", response.items().getFirst().name());
    }

    @Test
    void searchByEmailFilter() {
        User admin = this.repository.userRepo.findByName("admin").orElseThrow();

        LocationSearchResponse response = this.locationService.search(
                admin,
                LocationKind.USER,
                new ObjectSearchRequest(
                        null,
                        List.of(new SearchClause(new ResourceIdentifier(BuiltinPlugin.BUILTIN_PLUGIN_NAME, BuiltinPropertyIds.EMAIL), SearchOperator.IS_NULL, null)),
                        null,
                        null,
                        null,
                        null
                )
        );

        assertTrue(response.items().stream().anyMatch(u -> "admin".equals(u.name())));
    }
}
