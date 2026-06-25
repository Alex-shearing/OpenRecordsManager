package com.openrecordsmanager.controllers;

import com.openrecordsmanager.api.list.ListDefinition;
import com.openrecordsmanager.model.ListElement;
import com.openrecordsmanager.model.ListType;
import com.openrecordsmanager.model.repositories.DataRepository;
import com.openrecordsmanager.resources.ExpressionsService;
import com.openrecordsmanager.resources.ResourceCatalog;
import com.openrecordsmanager.resources.ResourceIdentifier;
import com.openrecordsmanager.resources.types.ResourceTypes;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.util.Objects;
import java.util.Optional;
import java.util.Set;
import java.util.stream.Collectors;

@RestController
@RequestMapping("/list")
public class ListController {

    private final ResourceCatalog registry;
    private final DataRepository repository;
    private final ExpressionsService expressions;

    public ListController(ResourceCatalog registry, DataRepository repository, ExpressionsService expressions) {
        this.registry = registry;
        this.repository = repository;
        this.expressions = expressions;
    }

    @GetMapping("")
    public ResponseEntity<ApiResponse<Set<ResourceIdentifier>>> getLists() {
        return ResponseEntity.ok(ApiResponse.success(this.repository.listTypeRepo.findAll().stream().map(listType -> listType.id).collect(Collectors.toSet())));
    }

    @GetMapping("/{list}")
    public ResponseEntity<ApiResponse<ListType>> getList(@PathVariable("list") ResourceIdentifier listType) {
        Optional<ListType> type = this.repository.listTypeRepo.findById(listType);
        if (type.isEmpty()) {
            return ResponseEntity.status(HttpStatus.NOT_FOUND).body(ApiResponse.error("list_not_found"));
        }
        return ResponseEntity.ok(ApiResponse.success(type.get()));
    }

    @GetMapping("/{list}/{element}")
    public ResponseEntity<ApiResponse<ListElement>> getListElement(@PathVariable("list") ResourceIdentifier listType, @PathVariable("element") ResourceIdentifier listElement) {
        Optional<ListType> type = this.repository.listTypeRepo.findById(listType);
        if (type.isEmpty()) {
            return ResponseEntity.status(HttpStatus.NOT_FOUND).body(ApiResponse.error("list_not_found"));
        }
        Optional<ListElement> el = type.get().children.stream().filter(listElement1 -> Objects.equals(listElement1.id, listElement)).findFirst();
        if (el.isEmpty()) {
            return ResponseEntity.status(HttpStatus.NOT_FOUND).body(ApiResponse.error("list_element_not_found"));
        }
        return ResponseEntity.ok(ApiResponse.success(el.get()));
    }

    @GetMapping("/{list}/search")
    public ResponseEntity<ApiResponse<Set<ListElement>>> searchListElement(@PathVariable("list") ResourceIdentifier listType, @RequestParam("value") String value) {
        Optional<ListType> type = this.repository.listTypeRepo.findById(listType);
        if (type.isEmpty()) {
            return ResponseEntity.status(HttpStatus.NOT_FOUND).body(ApiResponse.error("list_not_found"));
        }
        Set<ListElement> el = type.get().children.stream().filter(listElement1 -> listElement1.id.toString().contains(value)).collect(Collectors.toSet());
        if (el.isEmpty()) {
            return ResponseEntity.status(HttpStatus.NOT_FOUND).body(ApiResponse.error("list_element_not_found"));
        }
        return ResponseEntity.ok(ApiResponse.success(el));
    }

    @GetMapping("/template")
    public ResponseEntity<ApiResponse<Set<ResourceIdentifier>>> getTemplates() {
        return ResponseEntity.ok(ApiResponse.success(this.registry.getIds(ResourceTypes.LIST)));
    }

    @GetMapping("/template/{template}")
    public ResponseEntity<ApiResponse<ListDefinition>> getTemplate(@PathVariable("template") ResourceIdentifier listId) {
        ListDefinition listDef = this.registry.getComponent(ResourceTypes.LIST, listId);
        if (listDef == null) {
            return ResponseEntity.status(HttpStatus.NOT_FOUND).body(ApiResponse.error("list_template_not_found"));
        }
        return ResponseEntity.ok(ApiResponse.success(listDef));
    }

    @PostMapping("/template/{template}/apply")
    public ResponseEntity<ApiResponse<ListType>> applyList(@PathVariable("template") ResourceIdentifier listId) {
        ListDefinition listDef = this.registry.getComponent(ResourceTypes.LIST, listId);
        if (listDef == null) {
            return ResponseEntity.status(HttpStatus.NOT_FOUND).body(ApiResponse.error("list_template_not_found"));
        }

        ListType type = ResourceTypes.LIST.register(this.repository, this.registry, this.expressions, listId, listDef, false);

        listDef.defaultEntries.forEach((s, listItem) -> {
            ResourceIdentifier id = new ResourceIdentifier(listId.source(), s);
            ResourceTypes.LIST_ELEMENT.register(this.repository, this.registry, this.expressions, id, listItem, false);
        });

        return ResponseEntity.ok(ApiResponse.success(type));
    }

}
