package com.openrecordsmanager.controllers;

import com.openrecordsmanager.api.ResourceIdentifier;
import com.openrecordsmanager.api.types.ComponentTypes;
import com.openrecordsmanager.controllers.repsonse.InternalServerErrorApiResponse;
import com.openrecordsmanager.controllers.repsonse.NotFoundApiResponse;
import com.openrecordsmanager.controllers.repsonse.errors.ApiError;
import com.openrecordsmanager.model.ListElement;
import com.openrecordsmanager.model.ListType;
import com.openrecordsmanager.model.repositories.DataRepository;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.responses.ApiResponse;
import org.springframework.http.MediaType;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.web.bind.annotation.*;

import java.util.Objects;
import java.util.Set;
import java.util.stream.Collectors;

@RestController
@RequestMapping("/lists")
@InternalServerErrorApiResponse
@ApiResponse(responseCode = "200")
public class ListController {

    private final DataRepository repository;

    public ListController(DataRepository repository) {
        this.repository = repository;
    }

    @GetMapping(produces = MediaType.APPLICATION_JSON_VALUE)
    @Operation(summary = "Get all list type identifiers")
    @Transactional(readOnly = true)
    public Set<ResourceIdentifier> getLists() {
        return this.repository.listTypeRepo.findAll().stream()
                .map(listType -> listType.id).collect(Collectors.toSet());
    }

    @GetMapping(value = "/{list}", produces = MediaType.APPLICATION_JSON_VALUE)
    @Operation(summary = "Get list details")
    @NotFoundApiResponse
    @Transactional(readOnly = true)
    public ListType getList(@PathVariable("list") ResourceIdentifier listType) {
        return this.repository.listTypeRepo.findById(listType)
                .orElseThrow(() -> ApiError.notFound(ComponentTypes.LIST, listType));
    }

    @GetMapping(value = "/{list}/{element}", produces = MediaType.APPLICATION_JSON_VALUE)
    @Operation(summary = "Get list element details")
    @NotFoundApiResponse
    @Transactional(readOnly = true)
    public ListElement getListElement(@PathVariable("list") ResourceIdentifier listType, @PathVariable("element") ResourceIdentifier listElement) {
        ListType type = this.repository.listTypeRepo.findById(listType)
                .orElseThrow(() -> ApiError.notFound(ComponentTypes.LIST, listType));

        return type.children.stream()
                .filter(listElement1 -> Objects.equals(listElement1.id, listElement))
                .findFirst()
                .orElseThrow(() -> ApiError.notFound(ComponentTypes.LIST_ELEMENT, listElement));
    }

    @GetMapping(value = "/{list}/search", produces = MediaType.APPLICATION_JSON_VALUE)
    @Operation(summary = "Search for list elements in a list")
    @NotFoundApiResponse
    @Transactional(readOnly = true)
    public Set<ListElement> searchListElement(@PathVariable("list") ResourceIdentifier listType, @RequestParam("value") String value) {
        ListType type = this.repository.listTypeRepo.findById(listType)
                .orElseThrow(() -> ApiError.notFound(ComponentTypes.LIST, listType));

        return type.children.stream()
                .filter(listElement1 -> listElement1.id.toString().contains(value))
                .collect(Collectors.toSet());
    }

}
