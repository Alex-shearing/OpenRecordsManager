package com.openrecordsmanager.auth;

import com.openrecordsmanager.api.builtin.BuiltinConfigs;
import com.openrecordsmanager.database.SqliteTestSupport;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.boot.webmvc.test.autoconfigure.AutoConfigureMockMvc;
import org.springframework.test.context.DynamicPropertyRegistry;
import org.springframework.test.context.DynamicPropertySource;
import org.springframework.test.web.servlet.MockMvc;

import java.util.UUID;

import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.header;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

@SpringBootTest
@AutoConfigureMockMvc
class RedirectAuthCallbackIntegrationTest {

    @DynamicPropertySource
    static void properties(DynamicPropertyRegistry registry) {
        SqliteTestSupport.registerPrimaryMemoryDatabase(registry, RedirectAuthCallbackIntegrationTest.class);
        registry.add(BuiltinConfigs.PLUGINS_SKIP_SYNC.key(), () -> "true");
        registry.add(BuiltinConfigs.COOKIE_SECURE.key(), () -> "false");
        registry.add(BuiltinConfigs.PUBLIC_BASE_URL.key(), () -> "http://localhost:8080");
    }

    @Autowired
    private MockMvc mockMvc;

    @Test
    void callbackWithoutPendingStateRedirectsToLoginError() throws Exception {
        this.mockMvc.perform(get("/api/auth/callback/" + UUID.randomUUID())
                        .queryParam("code", "abc")
                        .queryParam("state", "xyz"))
                .andExpect(status().isFound())
                .andExpect(header().string("Location", "/login?error=auth_failed"));
    }
}
