package com.openrecordsmanager.controllers;

import com.openrecordsmanager.controllers.errors.ApiError;
import com.openrecordsmanager.model.ListElement;
import com.openrecordsmanager.model.ListType;
import com.openrecordsmanager.model.repositories.DataRepository;
import com.openrecordsmanager.resources.ResourceIdentifier;
import com.openrecordsmanager.resources.types.ComponentTypes;
import org.springframework.web.bind.annotation.*;

import java.util.Objects;
import java.util.Set;
import java.util.stream.Collectors;

@RestController
@RequestMapping("/lists")
public class ListController {

    private final DataRepository repository;

    public ListController(DataRepository repository) {
        this.repository = repository;
    }

    @GetMapping
    public ApiResponse<Set<ResourceIdentifier>> getLists() {
        return ApiResponse.success(this.repository.listTypeRepo.findAll().stream()
                .map(listType -> listType.id).collect(Collectors.toSet()));
    }

    @GetMapping("/{list}")
    public ApiResponse<ListType> getList(@PathVariable("list") ResourceIdentifier listType) {
        ListType type = this.repository.listTypeRepo.findById(listType)
                .orElseThrow(() -> ApiError.notFound(ComponentTypes.LIST, listType));

        return ApiResponse.success(type);
    }

    @GetMapping("/{list}/{element}")
    public ApiResponse<ListElement> getListElement(@PathVariable("list") ResourceIdentifier listType, @PathVariable("element") ResourceIdentifier listElement) {
        ListType type = this.repository.listTypeRepo.findById(listType)
                .orElseThrow(() -> ApiError.notFound(ComponentTypes.LIST, listType));

        ListElement el = type.children.stream()
                .filter(listElement1 -> Objects.equals(listElement1.id, listElement))
                .findFirst()
                .orElseThrow(() -> ApiError.notFound(ComponentTypes.LIST_ELEMENT, listElement));

        return ApiResponse.success(el);
    }

    @GetMapping("/{list}/search")
    public ApiResponse<Set<ListElement>> searchListElement(@PathVariable("list") ResourceIdentifier listType, @RequestParam("value") String value) {
        ListType type = this.repository.listTypeRepo.findById(listType)
                .orElseThrow(() -> ApiError.notFound(ComponentTypes.LIST, listType));

        Set<ListElement> el = type.children.stream()
                .filter(listElement1 -> listElement1.id.toString().contains(value))
                .collect(Collectors.toSet());

        return ApiResponse.success(el);
    }

}
