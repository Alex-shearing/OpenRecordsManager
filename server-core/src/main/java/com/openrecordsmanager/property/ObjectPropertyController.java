package com.openrecordsmanager.property;

import com.openrecordsmanager.api.ResourceIdentifier;
import com.openrecordsmanager.property.dto.NewObjectPropertyRequest;
import com.openrecordsmanager.property.dto.ObjectPropertyResponse;
import com.openrecordsmanager.property.dto.SimpleObjectPropertyResponse;
import com.openrecordsmanager.property.dto.UpdateObjectPropertyRequest;
import com.openrecordsmanager.rest.swagger.ConflictApiResponse;
import com.openrecordsmanager.rest.swagger.DefaultApiResponses;
import com.openrecordsmanager.rest.swagger.NotFoundApiResponse;
import io.swagger.v3.oas.annotations.Operation;
import org.springframework.http.MediaType;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.*;

import java.util.Set;

@RestController
@RequestMapping("/api/object_properties")
@DefaultApiResponses
@PreAuthorize("isAuthenticated()")
public class ObjectPropertyController {

    private final ObjectPropertyService service;

    public ObjectPropertyController(ObjectPropertyService service) {
        this.service = service;
    }

    @GetMapping(produces = MediaType.APPLICATION_JSON_VALUE)
    @Operation(summary = "Get all object properties")
    public Set<SimpleObjectPropertyResponse> objectProperty_retrieveAll() {
        return this.service.getAll();
    }

    @GetMapping(value = "/{id}", produces = MediaType.APPLICATION_JSON_VALUE)
    @Operation(summary = "Get object property details")
    @NotFoundApiResponse
    public ObjectPropertyResponse objectProperty_retrieveOne(@PathVariable("id") ResourceIdentifier id) {
        return this.service.get(id);
    }

    @PostMapping(produces = MediaType.APPLICATION_JSON_VALUE)
    @Operation(summary = "Create a new object property")
    @NotFoundApiResponse
    @ConflictApiResponse
    public ObjectPropertyResponse objectProperty_create(@RequestBody NewObjectPropertyRequest input) {
        return this.service.create(input);
    }

    @PutMapping(value = "/{id}", produces = MediaType.APPLICATION_JSON_VALUE)
    @Operation(summary = "Update an object property")
    @NotFoundApiResponse
    public ObjectPropertyResponse objectProperty_update(
            @PathVariable("id") ResourceIdentifier id,
            @RequestBody UpdateObjectPropertyRequest input
    ) {
        return this.service.update(id, input);
    }

    @DeleteMapping(value = "/{id}", produces = MediaType.APPLICATION_JSON_VALUE)
    @Operation(summary = "Delete an object property")
    @NotFoundApiResponse
    @ConflictApiResponse
    public void objectProperty_delete(@PathVariable("id") ResourceIdentifier id) {
        this.service.delete(id);
    }
}
