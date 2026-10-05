package com.openrecordsmanager.record;

import com.openrecordsmanager.api.ResourceIdentifier;
import com.openrecordsmanager.api.builtin.BuiltinConfigs;
import com.openrecordsmanager.api.builtin.BuiltinPlugin;
import com.openrecordsmanager.api.builtin.BuiltinPropertyIds;
import com.openrecordsmanager.api.search.SearchClause;
import com.openrecordsmanager.api.search.SearchOperator;
import com.openrecordsmanager.api.template.property.PropertyType;
import com.openrecordsmanager.api.template.recordtype.SecurityFilterUsage;
import com.openrecordsmanager.database.DataRepository;
import com.openrecordsmanager.database.SqliteTestSupport;
import com.openrecordsmanager.location.user.User;
import com.openrecordsmanager.property.ObjectProperty;
import com.openrecordsmanager.record.dto.RecordSearchResponse;
import com.openrecordsmanager.search.dto.ObjectSearchRequest;
import com.openrecordsmanager.record.type.RecordType;
import com.openrecordsmanager.record.type.RecordTypeProperty;
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

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNotNull;

@SpringBootTest
class RecordSearchIntegrationTest {

    private static final ResourceIdentifier SEARCH_RECORD_TYPE =
            ResourceIdentifier.valueOf("test:search_record_type");
    private static final ResourceIdentifier CUSTOM_PROP =
            ResourceIdentifier.valueOf("test:search_custom_field");

    @DynamicPropertySource
    static void properties(DynamicPropertyRegistry registry) {
        SqliteTestSupport.registerPrimaryMemoryDatabase(registry, RecordSearchIntegrationTest.class);
        registry.add(BuiltinConfigs.PLUGINS_SKIP_SYNC.key(), () -> "true");
        registry.add(BuiltinConfigs.COOKIE_SECURE.key(), () -> "false");
    }

    @Autowired
    private RecordService recordService;

    @Autowired
    private DataRepository repository;

    @Autowired
    private TransactionTemplate transactionTemplate;

    private User admin;
    private RecordType searchType;
    private ObjectProperty<String> customProperty;

    @BeforeEach
    void setUp() {
        this.admin = this.repository.userRepo.findByName("admin").orElseThrow();
        this.searchType = this.ensureRecordType();
        this.customProperty = this.requireCustomProperty();
    }

    @Test
    void searchByDefaultQMatchesTitle() {
        this.repository.recordRepo.saveAndFlush(new Record("Alpha Search Target", this.searchType));
        this.repository.recordRepo.saveAndFlush(new Record("Unrelated Document", this.searchType));

        RecordSearchResponse response = this.recordService.search(
                this.admin,
                new ObjectSearchRequest("Alpha Search", null, null, SEARCH_RECORD_TYPE, null, null)
        );

        assertEquals(1, response.items().size());
        assertEquals(
                "Alpha Search Target",
                response.items().getFirst().properties().get(new ResourceIdentifier(BuiltinPlugin.BUILTIN_PLUGIN_NAME, BuiltinPropertyIds.TITLE).toString()).asString()
        );
        String idKey = new ResourceIdentifier(BuiltinPlugin.BUILTIN_PLUGIN_NAME, BuiltinPropertyIds.ID).toString();
        assertNotNull(response.items().getFirst().properties().get(idKey));
        assertEquals(
                response.items().getFirst().id().toString(),
                response.items().getFirst().properties().get(idKey).asString()
        );
    }

    @Test
    void searchByPropertyFilterEquals() {
        this.repository.recordRepo.saveAndFlush(new Record("Exact Filter Title", this.searchType));

        RecordSearchResponse response = this.recordService.search(
                this.admin,
                new ObjectSearchRequest(
                        null,
                        List.of(new SearchClause(
                                new ResourceIdentifier(BuiltinPlugin.BUILTIN_PLUGIN_NAME, BuiltinPropertyIds.TITLE),
                                SearchOperator.EQ,
                                JsonNodeFactory.instance.stringNode("Exact Filter Title")
                        )),
                        null,
                        null,
                        null,
                        null
                )
        );

        assertEquals(1, response.items().size());
        assertEquals(
                "Exact Filter Title",
                response.items().getFirst().properties().get(new ResourceIdentifier(BuiltinPlugin.BUILTIN_PLUGIN_NAME, BuiltinPropertyIds.TITLE).toString()).asString()
        );
    }

    @Test
    void searchByNonBuiltinPropertyFilterEquals() {
        Record match = new Record("Match Record", this.searchType);
        match.setProperty(this.customProperty, "needle-value");
        this.repository.recordRepo.saveAndFlush(match);

        Record other = new Record("Other Record", this.searchType);
        other.setProperty(this.customProperty, "different-value");
        this.repository.recordRepo.saveAndFlush(other);

        RecordSearchResponse response = this.recordService.search(
                this.admin,
                new ObjectSearchRequest(
                        null,
                        List.of(new SearchClause(
                                CUSTOM_PROP,
                                SearchOperator.EQ,
                                JsonNodeFactory.instance.stringNode("needle-value")
                        )),
                        null,
                        null,
                        null,
                        null
                )
        );

        assertEquals(1, response.items().size());
        assertEquals(match.getId(), response.items().getFirst().id());
        assertEquals(
                "needle-value",
                response.items().getFirst().properties().get(CUSTOM_PROP.toString()).asString()
        );
    }

    @SuppressWarnings("unchecked")
    private RecordType ensureRecordType() {
        return this.transactionTemplate.execute(_ -> {
            ObjectProperty<String> titleProperty = this.repository.objectPropertyRepo.findById(new ResourceIdentifier(BuiltinPlugin.BUILTIN_PLUGIN_NAME, BuiltinPropertyIds.TITLE))
                    .map(p -> (ObjectProperty<String>) p)
                    .orElseGet(() -> this.repository.objectPropertyRepo.saveAndFlush(new ObjectProperty<>(
                            new ResourceIdentifier(BuiltinPlugin.BUILTIN_PLUGIN_NAME, BuiltinPropertyIds.TITLE),
                            PropertyType.STRING
                    )));

            ObjectProperty<String> customProperty = this.repository.objectPropertyRepo.findById(CUSTOM_PROP)
                    .map(p -> (ObjectProperty<String>) p)
                    .orElseGet(() -> this.repository.objectPropertyRepo.saveAndFlush(new ObjectProperty<>(
                            CUSTOM_PROP,
                            PropertyType.STRING
                    )));

            RecordType type = this.repository.recordTypeRepo.findById(SEARCH_RECORD_TYPE).orElseGet(() ->
                    this.repository.recordTypeRepo.saveAndFlush(new RecordType(SEARCH_RECORD_TYPE, null,
                            null,
                            SecurityFilterUsage.SHOW_ALL,
                            Set.of(
                                    new RecordTypeProperty<>(titleProperty, null),
                                    new RecordTypeProperty<>(customProperty, null)
                            )
                    ))
            );
            type.getProperties().size();
            return type;
        });
    }

    @SuppressWarnings("unchecked")
    private ObjectProperty<String> requireCustomProperty() {
        return (ObjectProperty<String>) this.repository.objectPropertyRepo.findById(CUSTOM_PROP)
                .orElseThrow(() -> new IllegalStateException("Missing custom search property: " + CUSTOM_PROP));
    }
}
