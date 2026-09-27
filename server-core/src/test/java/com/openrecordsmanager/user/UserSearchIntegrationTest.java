package com.openrecordsmanager.user;

import com.openrecordsmanager.api.builtin.BuiltinConfigs;
import com.openrecordsmanager.api.builtin.BuiltinPropertyIds;
import com.openrecordsmanager.api.search.SearchClause;
import com.openrecordsmanager.api.search.SearchOperator;
import com.openrecordsmanager.database.DataRepository;
import com.openrecordsmanager.database.SqliteTestSupport;
import com.openrecordsmanager.user.dto.UserSearchRequest;
import com.openrecordsmanager.user.dto.UserSearchResponse;
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
    private UserService userService;

    @Autowired
    private DataRepository repository;

    @Test
    void searchByUsernameDefaultQ() {
        User admin = this.repository.userRepo.findByUsername("admin").orElseThrow();

        UserSearchResponse response = this.userService.search(
                admin,
                new UserSearchRequest("admin", null, null, null, null)
        );

        assertEquals(1, response.items().size());
        assertEquals("admin", response.items().getFirst().username());
    }

    @Test
    void searchByEmailFilter() {
        User admin = this.repository.userRepo.findByUsername("admin").orElseThrow();

        UserSearchResponse response = this.userService.search(
                admin,
                new UserSearchRequest(
                        null,
                        List.of(new SearchClause(BuiltinPropertyIds.EMAIL, SearchOperator.IS_NULL, null)),
                        null,
                        null,
                        null
                )
        );

        assertTrue(response.items().stream().anyMatch(u -> u.username().equals("admin")));
    }
}
