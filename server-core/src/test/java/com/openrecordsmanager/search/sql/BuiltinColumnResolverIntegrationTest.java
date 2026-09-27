package com.openrecordsmanager.search.sql;

import com.openrecordsmanager.api.builtin.BuiltinConfigs;
import com.openrecordsmanager.api.builtin.BuiltinPropertyIds;
import com.openrecordsmanager.api.search.SearchFieldTarget;
import com.openrecordsmanager.database.SqliteTestSupport;
import com.openrecordsmanager.record.Record;
import com.openrecordsmanager.user.User;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.test.context.DynamicPropertyRegistry;
import org.springframework.test.context.DynamicPropertySource;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertTrue;

@SpringBootTest
class BuiltinColumnResolverIntegrationTest {

    @DynamicPropertySource
    static void properties(DynamicPropertyRegistry registry) {
        SqliteTestSupport.registerPrimaryMemoryDatabase(registry, BuiltinColumnResolverIntegrationTest.class);
        registry.add(BuiltinConfigs.PLUGINS_SKIP_SYNC.key(), () -> "true");
        registry.add(BuiltinConfigs.COOKIE_SECURE.key(), () -> "false");
    }

    @Autowired
    private BuiltinColumnResolver columnResolver;

    @Test
    void searchSchemasUseResolvedColumnsAndDefaultFields() {
        ObjectSearchSchema record = ObjectSearchSchema.of(
                SearchFieldTarget.RECORD,
                Record.class,
                Record.BUILTIN_PROPERTY_BINDINGS,
                this.columnResolver
        );
        assertEquals("record", record.tableName());
        assertEquals("id", record.idColumn());
        assertEquals("record_property_value", record.propertyValueTable());
        assertEquals("record_id", record.propertyValueFk());
        assertEquals("type_id", record.typeColumn());
        assertEquals("title", record.builtinColumns().get(BuiltinPropertyIds.TITLE).sqlColumn());
        assertEquals("mime_types", record.builtinColumns().get(BuiltinPropertyIds.MIME_TYPES).sqlColumn());
        assertTrue(record.builtinColumns().get(BuiltinPropertyIds.MIME_TYPES).jsonStored());
        assertEquals(
                java.util.Set.of(
                        BuiltinPropertyIds.TITLE,
                        BuiltinPropertyIds.KEYWORDS,
                        BuiltinPropertyIds.NOTES
                ),
                java.util.Set.copyOf(record.defaultSearchFields())
        );

        ObjectSearchSchema user = ObjectSearchSchema.of(
                SearchFieldTarget.USER,
                User.class,
                User.BUILTIN_PROPERTY_BINDINGS,
                this.columnResolver
        );
        assertEquals("user_details", user.tableName());
        assertEquals("id", user.idColumn());
        assertEquals("user_property_value", user.propertyValueTable());
        assertEquals("user_id", user.propertyValueFk());
        assertEquals(null, user.typeColumn());
        assertEquals("username", user.builtinColumns().get(BuiltinPropertyIds.USERNAME).sqlColumn());
        assertEquals("given_name", user.builtinColumns().get(BuiltinPropertyIds.GIVEN_NAME).sqlColumn());
        assertEquals(
                java.util.Set.of(
                        BuiltinPropertyIds.USERNAME,
                        BuiltinPropertyIds.GIVEN_NAME,
                        BuiltinPropertyIds.SURNAME,
                        BuiltinPropertyIds.EMAIL
                ),
                java.util.Set.copyOf(user.defaultSearchFields())
        );
    }
}
