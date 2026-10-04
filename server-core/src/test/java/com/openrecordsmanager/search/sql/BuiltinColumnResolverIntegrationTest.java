package com.openrecordsmanager.search.sql;

import com.openrecordsmanager.api.ResourceIdentifier;
import com.openrecordsmanager.api.builtin.BuiltinConfigs;
import com.openrecordsmanager.api.builtin.BuiltinPlugin;
import com.openrecordsmanager.api.builtin.BuiltinPropertyIds;
import com.openrecordsmanager.api.search.SearchFieldTarget;
import com.openrecordsmanager.database.SqliteTestSupport;
import com.openrecordsmanager.location.user.User;
import com.openrecordsmanager.record.Record;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.test.context.DynamicPropertyRegistry;
import org.springframework.test.context.DynamicPropertySource;

import java.util.List;
import java.util.Set;

import static org.junit.jupiter.api.Assertions.*;

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
        assertEquals(new QualifiedSqlColumn("record", "id"), record.primaryKey());
        assertEquals(List.of(), record.joins());
        assertEquals(new QualifiedSqlColumn("record_property_value", "record_id"), record.holderFk());
        assertEquals(new QualifiedSqlColumn("record_property_value", "property_id"), record.propertyId());
        assertEquals(new QualifiedSqlColumn("record_property_value", "property_value"), record.propertyValue());
        assertEquals(new QualifiedSqlColumn("record", "type_id"), record.typeColumn());
        assertEquals(
                new QualifiedSqlColumn("record", "title"),
                record.builtinColumns().get(new ResourceIdentifier(BuiltinPlugin.BUILTIN_PLUGIN_NAME, BuiltinPropertyIds.TITLE)).column()
        );
        assertEquals(
                new QualifiedSqlColumn("record", "mime_types"),
                record.builtinColumns().get(new ResourceIdentifier(BuiltinPlugin.BUILTIN_PLUGIN_NAME, BuiltinPropertyIds.MIME_TYPES)).column()
        );
        assertTrue(record.builtinColumns().get(new ResourceIdentifier(BuiltinPlugin.BUILTIN_PLUGIN_NAME, BuiltinPropertyIds.MIME_TYPES)).jsonStored());
        assertEquals(
                Set.of(
                        new ResourceIdentifier(BuiltinPlugin.BUILTIN_PLUGIN_NAME, BuiltinPropertyIds.TITLE),
                        new ResourceIdentifier(BuiltinPlugin.BUILTIN_PLUGIN_NAME, BuiltinPropertyIds.KEYWORDS),
                        new ResourceIdentifier(BuiltinPlugin.BUILTIN_PLUGIN_NAME, BuiltinPropertyIds.NOTES)
                ),
                Set.copyOf(record.defaultSearchFields())
        );

        ObjectSearchSchema user = ObjectSearchSchema.of(
                SearchFieldTarget.USER,
                User.class,
                User.BUILTIN_PROPERTY_BINDINGS,
                this.columnResolver
        );
        assertEquals(new QualifiedSqlColumn("user_details", "id"), user.primaryKey());
        assertEquals(List.of(new QualifiedSqlColumn("location", "id")), user.joins());
        assertEquals(new QualifiedSqlColumn("location_property_value", "location_id"), user.holderFk());
        assertEquals(new QualifiedSqlColumn("location_property_value", "property_id"), user.propertyId());
        assertEquals(new QualifiedSqlColumn("location_property_value", "property_value"), user.propertyValue());
        assertEquals(new QualifiedSqlColumn("location", "type_id"), user.typeColumn());
        assertEquals(
                new QualifiedSqlColumn("user_details", "username"),
                user.builtinColumns().get(new ResourceIdentifier(BuiltinPlugin.BUILTIN_PLUGIN_NAME, BuiltinPropertyIds.USERNAME)).column()
        );
        assertEquals(
                new QualifiedSqlColumn("user_details", "given_name"),
                user.builtinColumns().get(new ResourceIdentifier(BuiltinPlugin.BUILTIN_PLUGIN_NAME, BuiltinPropertyIds.GIVEN_NAME)).column()
        );
        assertEquals(
                new QualifiedSqlColumn("location", "name"),
                user.builtinColumns().get(new ResourceIdentifier(BuiltinPlugin.BUILTIN_PLUGIN_NAME, BuiltinPropertyIds.NAME)).column()
        );
        assertEquals(
                Set.of(
                        new ResourceIdentifier(BuiltinPlugin.BUILTIN_PLUGIN_NAME, BuiltinPropertyIds.NAME),
                        new ResourceIdentifier(BuiltinPlugin.BUILTIN_PLUGIN_NAME, BuiltinPropertyIds.USERNAME),
                        new ResourceIdentifier(BuiltinPlugin.BUILTIN_PLUGIN_NAME, BuiltinPropertyIds.GIVEN_NAME),
                        new ResourceIdentifier(BuiltinPlugin.BUILTIN_PLUGIN_NAME, BuiltinPropertyIds.SURNAME),
                        new ResourceIdentifier(BuiltinPlugin.BUILTIN_PLUGIN_NAME, BuiltinPropertyIds.EMAIL)
                ),
                Set.copyOf(user.defaultSearchFields())
        );
    }
}
