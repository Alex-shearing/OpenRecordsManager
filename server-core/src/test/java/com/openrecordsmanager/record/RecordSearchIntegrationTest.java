package com.openrecordsmanager.record;

import com.openrecordsmanager.api.ResourceIdentifier;
import com.openrecordsmanager.api.builtin.BuiltinConfigs;
import com.openrecordsmanager.api.builtin.BuiltinPropertyIds;
import com.openrecordsmanager.api.search.SearchClause;
import com.openrecordsmanager.api.search.SearchOperator;
import com.openrecordsmanager.api.template.property.PropertyType;
import com.openrecordsmanager.api.template.recordtype.SecurityFilterUsage;
import com.openrecordsmanager.database.DataRepository;
import com.openrecordsmanager.database.SqliteTestSupport;
import com.openrecordsmanager.property.ObjectProperty;
import com.openrecordsmanager.record.dto.RecordSearchRequest;
import com.openrecordsmanager.record.dto.RecordSearchResponse;
import com.openrecordsmanager.recordtype.RecordType;
import com.openrecordsmanager.recordtype.RecordTypeProperty;
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

import static org.junit.jupiter.api.Assertions.assertEquals;

@SpringBootTest
class RecordSearchIntegrationTest {

    private static final ResourceIdentifier SEARCH_RECORD_TYPE =
            ResourceIdentifier.valueOf("test:search_record_type");

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

    @BeforeEach
    void setUp() {
        this.admin = this.repository.userRepo.findByUsername("admin").orElseThrow();
        this.searchType = this.ensureRecordType();
    }

    @Test
    void searchByDefaultQMatchesTitle() {
        this.repository.recordRepo.saveAndFlush(new Record("Alpha Search Target", this.searchType));
        this.repository.recordRepo.saveAndFlush(new Record("Unrelated Document", this.searchType));

        RecordSearchResponse response = this.recordService.search(
                this.admin,
                new RecordSearchRequest("Alpha Search", null, null, SEARCH_RECORD_TYPE, null, null)
        );

        assertEquals(1, response.items().size());
        assertEquals(
                "Alpha Search Target",
                response.items().getFirst().properties().get(BuiltinPropertyIds.TITLE.toString()).asString()
        );
    }

    @Test
    void searchByPropertyFilterEquals() {
        this.repository.recordRepo.saveAndFlush(new Record("Exact Filter Title", this.searchType));

        RecordSearchResponse response = this.recordService.search(
                this.admin,
                new RecordSearchRequest(
                        null,
                        List.of(new SearchClause(
                                BuiltinPropertyIds.TITLE,
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
                response.items().getFirst().properties().get(BuiltinPropertyIds.TITLE.toString()).asString()
        );
    }

    @SuppressWarnings("unchecked")
    private RecordType ensureRecordType() {
        return this.transactionTemplate.execute(status -> {
            ObjectProperty<String> titleProperty = this.repository.objectPropertyRepo.findById(BuiltinPropertyIds.TITLE)
                    .map(p -> (ObjectProperty<String>) p)
                    .orElseGet(() -> this.repository.objectPropertyRepo.saveAndFlush(new ObjectProperty<>(
                            BuiltinPropertyIds.TITLE,
                            "Title",
                            "Title",
                            PropertyType.STRING
                    )));

            RecordType type = this.repository.recordTypeRepo.findById(SEARCH_RECORD_TYPE).orElseGet(() ->
                    this.repository.recordTypeRepo.saveAndFlush(new RecordType(
                            SEARCH_RECORD_TYPE,
                            "Search record type",
                            "Record type for search tests",
                            null,
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
