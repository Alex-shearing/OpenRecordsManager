package com.openrecordsmanager.controllers;

import com.openrecordsmanager.list.ListDefinition;
import com.openrecordsmanager.model.ListElement;
import com.openrecordsmanager.model.ListType;
import com.openrecordsmanager.model.repositories.ListElementRepository;
import com.openrecordsmanager.model.repositories.ListTypeRepository;
import com.openrecordsmanager.resources.ResourceIdentifier;
import com.openrecordsmanager.resources.ResourceRegistry;
import com.openrecordsmanager.resources.ResourceType;
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

    private final ResourceRegistry resourceRegistry;
    private final ListTypeRepository listTypeRepository;
    private final ListElementRepository listElementRepository;

    public ListController(ResourceRegistry resourceRegistry, ListTypeRepository listTypeRepository, ListElementRepository listElementRepository) {
        this.resourceRegistry = resourceRegistry;
        this.listTypeRepository = listTypeRepository;
        this.listElementRepository = listElementRepository;
    }

    @GetMapping("")
    public ResponseEntity<ApiResponse<Set<ResourceIdentifier>>> getLists() {
        return ResponseEntity.ok(ApiResponse.success(this.listTypeRepository.findAll().stream().map(listType -> listType.id.getId()).collect(Collectors.toSet())));
    }

    @GetMapping("/{list}")
    public ResponseEntity<ApiResponse<ListType>> getList(@PathVariable("list") ResourceIdentifier listType) {
        Optional<ListType> type = this.listTypeRepository.findById(listType);
        if (type.isEmpty()) {
            return ResponseEntity.status(HttpStatus.NOT_FOUND).body(ApiResponse.error("list_not_found"));
        }
        return ResponseEntity.ok(ApiResponse.success(type.get()));
    }

    @GetMapping("/{list}/{element}")
    public ResponseEntity<ApiResponse<ListElement>> getListElement(@PathVariable("list") ResourceIdentifier listType, @PathVariable("element") ResourceIdentifier listElement) {
        Optional<ListType> type = this.listTypeRepository.findById(listType);
        if (type.isEmpty()) {
            return ResponseEntity.status(HttpStatus.NOT_FOUND).body(ApiResponse.error("list_not_found"));
        }
        Optional<ListElement> el = type.get().children.stream().filter(listElement1 -> Objects.equals(listElement1.id.getId(), listElement)).findFirst();
        if (el.isEmpty()) {
            return ResponseEntity.status(HttpStatus.NOT_FOUND).body(ApiResponse.error("list_element_not_found"));
        }
        return ResponseEntity.ok(ApiResponse.success(el.get()));
    }

    @GetMapping("/{list}/search")
    public ResponseEntity<ApiResponse<Set<ListElement>>> searchListElement(@PathVariable("list") ResourceIdentifier listType, @RequestParam("value") String value) {
        Optional<ListType> type = this.listTypeRepository.findById(listType);
        if (type.isEmpty()) {
            return ResponseEntity.status(HttpStatus.NOT_FOUND).body(ApiResponse.error("list_not_found"));
        }
        Set<ListElement> el = type.get().children.stream().filter(listElement1 -> listElement1.id.getId().toString().contains(value)).collect(Collectors.toSet());
        if (el.isEmpty()) {
            return ResponseEntity.status(HttpStatus.NOT_FOUND).body(ApiResponse.error("list_element_not_found"));
        }
        return ResponseEntity.ok(ApiResponse.success(el));
    }

    @GetMapping("/template")
    public ResponseEntity<ApiResponse<Set<ResourceIdentifier>>> getTemplates() {
        return ResponseEntity.ok(ApiResponse.success(this.resourceRegistry.getIds(ResourceType.LIST)));
    }

    @GetMapping("/template/{template}")
    public ResponseEntity<ApiResponse<ListDefinition>> getTemplate(@PathVariable("template") ResourceIdentifier listId) {
        ListDefinition listDef = this.resourceRegistry.getComponent(ResourceType.LIST, listId);
        if (listDef == null) {
            return ResponseEntity.status(HttpStatus.NOT_FOUND).body(ApiResponse.error("list_template_not_found"));
        }
        return ResponseEntity.ok(ApiResponse.success(listDef));
    }

    @PostMapping("/template/{template}/apply")
    public ResponseEntity<ApiResponse<ListType>> applyList(@PathVariable("template") ResourceIdentifier listId) {
        ListDefinition listDef = this.resourceRegistry.getComponent(ResourceType.LIST, listId);
        if (listDef == null) {
            return ResponseEntity.status(HttpStatus.NOT_FOUND).body(ApiResponse.error("list_template_not_found"));
        }

        ListType type = this.listTypeRepository.findById(listId).orElse(new ListType()).fromDefinition(listDef);
        this.listTypeRepository.saveAndFlush(type);

        listDef.defaultEntries.forEach((s, listItem) -> {
            ResourceIdentifier id = new ResourceIdentifier(listId.source, s);
            ListElement ele = this.listElementRepository.findById(id).orElse(new ListElement()).fromDefinition(type, listItem);
            this.listElementRepository.saveAndFlush(ele);
        });

        return ResponseEntity.ok(ApiResponse.success(type));
    }

}
