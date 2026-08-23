package com.openrecordsmanager.list;

import com.openrecordsmanager.api.ResourceIdentifier;
import com.openrecordsmanager.list.dto.ListElementResponse;
import com.openrecordsmanager.list.dto.ListTypeResponse;
import com.openrecordsmanager.list.dto.SimpleListTypeResponse;
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

    @GetMapping(value = "/{list}", produces = MediaType.APPLICATION_JSON_VALUE)
    @Operation(summary = "Get list details")
    @NotFoundApiResponse
    public ListTypeResponse getList(@PathVariable("list") ResourceIdentifier listType) {
        return this.service.get(listType);
    }

    @GetMapping(value = "/{list}/{element}", produces = MediaType.APPLICATION_JSON_VALUE)
    @Operation(summary = "Get list element details")
    @NotFoundApiResponse
    public ListElementResponse getListElement(@PathVariable("list") ResourceIdentifier list, @PathVariable("element") ResourceIdentifier element) {
        return this.service.getElement(element, element);
    }

    @GetMapping(value = "/{list}/search", produces = MediaType.APPLICATION_JSON_VALUE)
    @Operation(summary = "Search for list elements in a list")
    @NotFoundApiResponse
    public Set<ListElementResponse> searchListElement(@PathVariable("list") ResourceIdentifier list, @RequestParam("value") String value) {
        return this.service.searchElement(list, value);
    }

}
