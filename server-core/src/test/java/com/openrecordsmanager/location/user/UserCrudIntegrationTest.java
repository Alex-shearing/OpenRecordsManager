package com.openrecordsmanager.location.user;

import com.openrecordsmanager.api.ResourceIdentifier;
import com.openrecordsmanager.api.builtin.BuiltinConfigs;
import com.openrecordsmanager.api.template.list.IListElement;
import com.openrecordsmanager.api.template.property.PropertyType;
import com.openrecordsmanager.audit.AuditTestSupport;
import com.openrecordsmanager.database.DataRepository;
import com.openrecordsmanager.database.SqliteTestSupport;
import com.openrecordsmanager.list.ListElement;
import com.openrecordsmanager.list.ListType;
import com.openrecordsmanager.property.ObjectProperty;
import com.openrecordsmanager.rest.exception.ResourceInUseException;
import com.openrecordsmanager.rest.exception.ResourceNotFoundException;
import com.openrecordsmanager.location.user.dto.NewUserRequest;
import com.openrecordsmanager.location.user.dto.UpdateUserRequest;
import com.openrecordsmanager.location.user.dto.UserResponse;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.test.context.DynamicPropertyRegistry;
import org.springframework.test.context.DynamicPropertySource;
import tools.jackson.databind.JsonNode;
import tools.jackson.databind.node.JsonNodeFactory;

import java.util.Collection;
import java.util.Map;
import java.util.Set;
import java.util.UUID;

import static org.junit.jupiter.api.Assertions.*;

@SpringBootTest
class UserCrudIntegrationTest {

    private static final ResourceIdentifier LIST_ID = ResourceIdentifier.valueOf("test:user_list_prop_list");
    private static final ResourceIdentifier ELEMENT_SECRET = ResourceIdentifier.valueOf("test:user_list_secret");
    private static final ResourceIdentifier ELEMENT_TOP_SECRET = ResourceIdentifier.valueOf("test:user_list_top_secret");
    private static final ResourceIdentifier LIST_ITEM_PROP = ResourceIdentifier.valueOf("test:user_list_item_prop");
    private static final ResourceIdentifier LIST_MULTI_PROP = ResourceIdentifier.valueOf("test:user_list_multi_prop");

    @DynamicPropertySource
    static void properties(DynamicPropertyRegistry registry) {
        SqliteTestSupport.registerPrimaryMemoryDatabase(registry, UserCrudIntegrationTest.class);
        registry.add(BuiltinConfigs.PLUGINS_SKIP_SYNC.key(), () -> "true");
        registry.add(BuiltinConfigs.COOKIE_SECURE.key(), () -> "false");
    }

    @Autowired
    private UserService userService;

    @Autowired
    private DataRepository repository;

    private User admin;

    @BeforeEach
    void setUpListProperties() {
        this.admin = this.repository.userRepo.findByUsername("admin").orElseThrow();

        ListType listType = this.repository.listTypeRepo.findById(LIST_ID).orElseGet(() ->
                this.repository.listTypeRepo.saveAndFlush(new ListType(LIST_ID))
        );

        this.repository.listElementRepo.findById(ELEMENT_SECRET).orElseGet(() ->
                this.repository.listElementRepo.saveAndFlush(
                        new ListElement(ELEMENT_SECRET, listType, 1, null, Set.of())
                )
        );
        this.repository.listElementRepo.findById(ELEMENT_TOP_SECRET).orElseGet(() ->
                this.repository.listElementRepo.saveAndFlush(
                        new ListElement(ELEMENT_TOP_SECRET, listType, 2, null, Set.of())
                )
        );

        if (this.repository.objectPropertyRepo.findById(LIST_ITEM_PROP).isEmpty()) {
            ObjectProperty<IListElement> itemProp = new ObjectProperty<>(
                    LIST_ITEM_PROP,
                    PropertyType.LIST_ITEM,
                    listType,
                    null,
                    null,
                    null,
                    false
            );
            this.repository.objectPropertyRepo.saveAndFlush(itemProp);
        }

        if (this.repository.objectPropertyRepo.findById(LIST_MULTI_PROP).isEmpty()) {
            ObjectProperty<Collection<IListElement>> multiProp = new ObjectProperty<>(
                    LIST_MULTI_PROP,
                    PropertyType.LIST_MULTIPLE,
                    listType,
                    null,
                    null,
                    null,
                    false
            );
            this.repository.objectPropertyRepo.saveAndFlush(multiProp);
        }
    }

    @Test
    void createGetAndUpdateUser() {
        String username = "integration_user_" + UUID.randomUUID().toString().substring(0, 8);

        UserResponse created = AuditTestSupport.withAudit(this.admin, () -> this.userService.create(
                new NewUserRequest(username, null, Map.of())
        ));

        assertEquals(username, created.username());
        assertNotNull(created.id());

        UserResponse loaded = this.userService.get(created.id());
        assertEquals(username, loaded.username());

        String updatedUsername = username + "_updated";
        UserResponse updated = AuditTestSupport.withAudit(this.admin, () -> this.userService.update(
                this.admin,
                created.id(),
                new UpdateUserRequest(updatedUsername, null, null, null)
        ));

        assertEquals(updatedUsername, updated.username());
        assertEquals(
                updatedUsername,
                this.repository.userRepo.findById(created.id()).orElseThrow().getName()
        );
    }

    @Test
    void setAndGetListItemProperties() {
        String username = "list_prop_user_" + UUID.randomUUID().toString().substring(0, 8);

        Map<ResourceIdentifier, JsonNode> createProps = Map.of(
                LIST_ITEM_PROP, JsonNodeFactory.instance.stringNode(ELEMENT_SECRET.toString()),
                LIST_MULTI_PROP, JsonNodeFactory.instance.arrayNode().add(ELEMENT_TOP_SECRET.toString())
        );

        UserResponse created = AuditTestSupport.withAudit(this.admin, () -> this.userService.create(
                new NewUserRequest(username, null, createProps)
        ));

        assertEquals(ELEMENT_SECRET.toString(), created.properties().get(LIST_ITEM_PROP.toString()).asString());
        assertEquals(ELEMENT_TOP_SECRET.toString(), created.properties().get(LIST_MULTI_PROP.toString()).get(0).asString());

        Map<ResourceIdentifier, JsonNode> updateProps = Map.of(
                LIST_ITEM_PROP, JsonNodeFactory.instance.stringNode(ELEMENT_TOP_SECRET.toString()),
                LIST_MULTI_PROP, JsonNodeFactory.instance.arrayNode()
                        .add(ELEMENT_SECRET.toString())
                        .add(ELEMENT_TOP_SECRET.toString())
        );

        UserResponse updated = AuditTestSupport.withAudit(this.admin, () -> this.userService.update(
                this.admin,
                created.id(),
                new UpdateUserRequest(null, null, null, updateProps)
        ));

        assertEquals(ELEMENT_TOP_SECRET.toString(), updated.properties().get(LIST_ITEM_PROP.toString()).asString());
        assertEquals(ELEMENT_SECRET.toString(), updated.properties().get(LIST_MULTI_PROP.toString()).get(0).asString());
        assertEquals(ELEMENT_TOP_SECRET.toString(), updated.properties().get(LIST_MULTI_PROP.toString()).get(1).asString());

        UserResponse loaded = this.userService.get(created.id());
        assertEquals(ELEMENT_TOP_SECRET.toString(), loaded.properties().get(LIST_ITEM_PROP.toString()).asString());
        assertEquals(ELEMENT_SECRET.toString(), loaded.properties().get(LIST_MULTI_PROP.toString()).get(0).asString());
        assertEquals(ELEMENT_TOP_SECRET.toString(), loaded.properties().get(LIST_MULTI_PROP.toString()).get(1).asString());
    }

    @Test
    void createDuplicateUsernameThrowsConflict() {
        String username = "duplicate_user_" + UUID.randomUUID().toString().substring(0, 8);
        NewUserRequest request = new NewUserRequest(username, null, Map.of());

        AuditTestSupport.withAudit(this.admin, () -> this.userService.create(request));

        assertThrows(ResourceInUseException.class, () -> AuditTestSupport.withAudit(this.admin, () -> this.userService.create(request)));
    }

    @Test
    void getUnknownUserThrowsNotFound() {
        assertThrows(ResourceNotFoundException.class, () -> this.userService.get(UUID.randomUUID()));
    }

}
