package com.openrecordsmanager.search;

import com.openrecordsmanager.api.ResourceIdentifier;
import com.openrecordsmanager.api.builtin.BuiltinConfigs;
import com.openrecordsmanager.api.builtin.BuiltinPropertyIds;
import com.openrecordsmanager.api.search.*;
import com.openrecordsmanager.api.template.property.PropertyType;
import com.openrecordsmanager.api.template.recordtype.SecurityFilterUsage;
import com.openrecordsmanager.api.types.ComponentTypes;
import com.openrecordsmanager.database.DataRepository;
import com.openrecordsmanager.database.SqliteTestSupport;
import com.openrecordsmanager.plugin.registry.ComponentCatalog;
import com.openrecordsmanager.property.ObjectProperty;
import com.openrecordsmanager.record.Record;
import com.openrecordsmanager.recordtype.RecordType;
import com.openrecordsmanager.recordtype.RecordTypeProperty;
import com.openrecordsmanager.search.sql.BuiltinColumnResolver;
import com.openrecordsmanager.search.sql.ObjectSearchSchema;
import com.openrecordsmanager.user.User;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.test.context.DynamicPropertyRegistry;
import org.springframework.test.context.DynamicPropertySource;
import org.springframework.transaction.support.TransactionTemplate;
import tools.jackson.databind.node.JsonNodeFactory;

import java.util.List;
import java.util.Set;
import java.util.UUID;
import java.util.stream.Stream;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertTrue;

@SpringBootTest
class ObjectSearchExecutorIntegrationTest {

    private static final ResourceIdentifier SEARCH_RECORD_TYPE =
            ResourceIdentifier.valueOf("test:executor_search_record_type");
    private static final ResourceIdentifier PLUGIN_FIELD =
            ResourceIdentifier.valueOf("test:executor_plugin_field");

    @DynamicPropertySource
    static void properties(DynamicPropertyRegistry registry) {
        SqliteTestSupport.registerPrimaryMemoryDatabase(registry, ObjectSearchExecutorIntegrationTest.class);
        registry.add(BuiltinConfigs.PLUGINS_SKIP_SYNC.key(), () -> "true");
        registry.add(BuiltinConfigs.COOKIE_SECURE.key(), () -> "false");
    }

    @Autowired
    private ObjectSearchExecutor searchExecutor;

    @Autowired
    private BuiltinColumnResolver columnResolver;

    @Autowired
    private DataRepository repository;

    @Autowired
    private ComponentCatalog catalog;

    @Autowired
    private TransactionTemplate transactionTemplate;

    private ObjectSearchSchema recordSchema;
    private User admin;

    @BeforeEach
    void setUp() {
        this.recordSchema = ObjectSearchSchema.of(
                SearchFieldTarget.RECORD,
                Record.class,
                Record.BUILTIN_PROPERTY_BINDINGS,
                this.columnResolver
        );
        this.admin = this.repository.userRepo.findByUsername("admin").orElseThrow();
        this.catalog.getRegistry(ComponentTypes.SEARCH_FIELD_PROVIDER).builder().build();
    }

    @Test
    void searchIdsAllModeIntersectsPluginProvider() {
        RecordType type = this.ensureRecordType();
        Record alpha = this.repository.recordRepo.saveAndFlush(new Record("Shared Alpha", type));
        Record beta = this.repository.recordRepo.saveAndFlush(new Record("Shared Beta", type));

        this.registerPluginProvider(Set.of(alpha.getId()));

        List<UUID> ids = this.searchExecutor.searchIds(
                this.recordSchema,
                this.admin,
                "Shared",
                List.of(new SearchClause(
                        PLUGIN_FIELD,
                        SearchOperator.EQ,
                        JsonNodeFactory.instance.stringNode("ignored")
                )),
                SearchMatchMode.ALL,
                SEARCH_RECORD_TYPE,
                null,
                50
        );

        assertEquals(List.of(alpha.getId()), ids);
        assertTrue(ids.stream().noneMatch(id -> id.equals(beta.getId())));
    }

    @Test
    void searchIdsAnyModeUnionsPluginProvider() {
        RecordType type = this.ensureRecordType();
        Record alpha = this.repository.recordRepo.saveAndFlush(new Record("UniqueAlpha Target", type));
        Record beta = this.repository.recordRepo.saveAndFlush(new Record("UniqueBeta Other", type));

        this.registerPluginProvider(Set.of(beta.getId()));

        List<UUID> ids = this.searchExecutor.searchIds(
                this.recordSchema,
                this.admin,
                "UniqueAlpha",
                List.of(new SearchClause(
                        PLUGIN_FIELD,
                        SearchOperator.EQ,
                        JsonNodeFactory.instance.stringNode("ignored")
                )),
                SearchMatchMode.ANY,
                SEARCH_RECORD_TYPE,
                null,
                50
        );

        assertEquals(Stream.of(alpha.getId(), beta.getId()).sorted().toList(), ids);
    }

    private void registerPluginProvider(Set<UUID> matchingIds) {
        SearchFieldProvider provider = new SearchFieldProvider(
                SearchFieldTarget.RECORD,
                Set.of(SearchOperator.EQ)
        ) {
            @Override
            public Set<UUID> findMatchingIds(SearchClause clause, SearchContext context) {
                return matchingIds;
            }
        };

        var builder = this.catalog.getRegistry(ComponentTypes.SEARCH_FIELD_PROVIDER).builder();
        builder.register(PLUGIN_FIELD, provider);
        builder.build();
    }

    @SuppressWarnings("unchecked")
    private RecordType ensureRecordType() {
        return this.transactionTemplate.execute(status -> {
            ObjectProperty<String> titleProperty = this.repository.objectPropertyRepo.findById(BuiltinPropertyIds.TITLE)
                    .map(p -> (ObjectProperty<String>) p)
                    .orElseGet(() -> this.repository.objectPropertyRepo.saveAndFlush(new ObjectProperty<>(
                            BuiltinPropertyIds.TITLE,
                            PropertyType.STRING
                    )));

            RecordType type = this.repository.recordTypeRepo.findById(SEARCH_RECORD_TYPE).orElseGet(() ->
                    this.repository.recordTypeRepo.saveAndFlush(new RecordType(SEARCH_RECORD_TYPE, null,
                            null,
                            SecurityFilterUsage.SHOW_ALL,
                            Set.of(new RecordTypeProperty<>(titleProperty, null))
                    ))
            );
            type.getProperties().size();
            return type;
        });
    }
}
