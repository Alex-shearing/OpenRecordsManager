package com.openrecordsmanager.list;

import com.openrecordsmanager.api.ResourceIdentifier;
import com.openrecordsmanager.list.dto.*;
import com.openrecordsmanager.rest.swagger.ConflictApiResponse;
import com.openrecordsmanager.rest.swagger.DefaultApiResponses;
import com.openrecordsmanager.rest.swagger.NotFoundApiResponse;
import io.swagger.v3.oas.annotations.Operation;
import org.springframework.http.MediaType;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.*;

import java.util.Set;

@RestController
@RequestMapping("/api/lists")
@DefaultApiResponses
@PreAuthorize("isAuthenticated()")
public class ListController {

    private final ListService service;

    public ListController(ListService service) {
        this.service = service;
    }

    @GetMapping(produces = MediaType.APPLICATION_JSON_VALUE)
    @Operation(summary = "Get all list type identifiers")
    public Set<SimpleListTypeResponse> getLists() {
        return this.service.getAll();
    }

    @PostMapping(produces = MediaType.APPLICATION_JSON_VALUE)
    @Operation(summary = "Create a new list type")
    @ConflictApiResponse
    public ListTypeResponse createList(@RequestBody NewListTypeRequest input) {
        return this.service.create(input);
    }

    @GetMapping(value = "/{list}", produces = MediaType.APPLICATION_JSON_VALUE)
    @Operation(summary = "Get list details")
    @NotFoundApiResponse
    public ListTypeResponse getList(@PathVariable("list") ResourceIdentifier listType) {
        return this.service.get(listType);
    }

    @PutMapping(value = "/{list}", produces = MediaType.APPLICATION_JSON_VALUE)
    @Operation(summary = "Update a list type")
    @NotFoundApiResponse
    public ListTypeResponse updateList(
            @PathVariable("list") ResourceIdentifier listType,
            @RequestBody UpdateListTypeRequest input
    ) {
        return this.service.update(listType, input);
    }

    @DeleteMapping(value = "/{list}", produces = MediaType.APPLICATION_JSON_VALUE)
    @Operation(summary = "Delete a list type")
    @NotFoundApiResponse
    @ConflictApiResponse
    public void deleteList(@PathVariable("list") ResourceIdentifier listType) {
        this.service.delete(listType);
    }

    @GetMapping(value = "/{list}/search", produces = MediaType.APPLICATION_JSON_VALUE)
    @Operation(summary = "Search for list elements in a list")
    @NotFoundApiResponse
    public Set<ListElementResponse> searchListElement(
            @PathVariable("list") ResourceIdentifier list,
            @RequestParam("value") String value
    ) {
        return this.service.searchElement(list, value);
    }

    @GetMapping(value = "/{list}/{element}", produces = MediaType.APPLICATION_JSON_VALUE)
    @Operation(summary = "Get list element details")
    @NotFoundApiResponse
    public ListElementResponse getListElement(
            @PathVariable("list") ResourceIdentifier list,
            @PathVariable("element") ResourceIdentifier element
    ) {
        return this.service.getElement(list, element);
    }

    @PostMapping(value = "/{list}", produces = MediaType.APPLICATION_JSON_VALUE)
    @Operation(summary = "Create a new list element")
    @NotFoundApiResponse
    @ConflictApiResponse
    public ListElementResponse createListElement(
            @PathVariable("list") ResourceIdentifier list,
            @RequestBody NewListElementRequest input
    ) {
        return this.service.createElement(list, input);
    }

    @PutMapping(value = "/{list}/{element}", produces = MediaType.APPLICATION_JSON_VALUE)
    @Operation(summary = "Update a list element")
    @NotFoundApiResponse
    public ListElementResponse updateListElement(
            @PathVariable("list") ResourceIdentifier list,
            @PathVariable("element") ResourceIdentifier element,
            @RequestBody UpdateListElementRequest input
    ) {
        return this.service.updateElement(list, element, input);
    }

    @DeleteMapping(value = "/{list}/{element}", produces = MediaType.APPLICATION_JSON_VALUE)
    @Operation(summary = "Delete a list element")
    @NotFoundApiResponse
    public void deleteListElement(
            @PathVariable("list") ResourceIdentifier list,
            @PathVariable("element") ResourceIdentifier element
    ) {
        this.service.deleteElement(list, element);
    }
}
