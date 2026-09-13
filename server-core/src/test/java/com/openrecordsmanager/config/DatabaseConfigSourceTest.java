package com.openrecordsmanager.config;

import org.junit.jupiter.api.Test;
import org.springframework.core.env.EnumerablePropertySource;
import org.springframework.jdbc.core.JdbcTemplate;

import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertNull;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.verify;

class DatabaseConfigSourceTest {

    @Test
    void isNotEnumerableAndSkipsServerKeys() {
        JdbcTemplate jdbcTemplate = mock(JdbcTemplate.class);
        DatabaseConfigSource source = new DatabaseConfigSource(jdbcTemplate);

        assertFalse(EnumerablePropertySource.class.isInstance(source));
        assertNull(source.getProperty("server.database.primary.url"));
        verify(jdbcTemplate).setQueryTimeout(2);
    }
}
