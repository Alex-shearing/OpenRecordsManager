package com.openrecordsmanager.location.relationship;

import com.openrecordsmanager.api.ResourceIdentifier;
import com.openrecordsmanager.location.relationship.dto.LocationRelationshipTypeResponse;
import com.openrecordsmanager.location.relationship.dto.NewLocationRelationshipTypeRequest;
import com.openrecordsmanager.location.relationship.dto.UpdateLocationRelationshipTypeRequest;
import com.openrecordsmanager.rest.swagger.ConflictApiResponse;
import com.openrecordsmanager.rest.swagger.DefaultApiResponses;
import com.openrecordsmanager.rest.swagger.NotFoundApiResponse;
import io.swagger.v3.oas.annotations.Operation;
import org.springframework.http.MediaType;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.*;

import java.util.Set;

@RestController
@RequestMapping("/api/location-relationship-type")
@DefaultApiResponses
@PreAuthorize("isAuthenticated()")
public class LocationRelationshipTypeController {

    private final LocationRelationshipTypeService service;

    public LocationRelationshipTypeController(LocationRelationshipTypeService service) {
        this.service = service;
    }

    @GetMapping(produces = MediaType.APPLICATION_JSON_VALUE)
    @Operation(summary = "List location relationship types")
    public Set<LocationRelationshipTypeResponse> listLocationRelationshipTypes() {
        return this.service.getAll();
    }

    @GetMapping(value = "/{id}", produces = MediaType.APPLICATION_JSON_VALUE)
    @Operation(summary = "Get a location relationship type")
    @NotFoundApiResponse
    public LocationRelationshipTypeResponse getLocationRelationshipType(@PathVariable("id") ResourceIdentifier id) {
        return this.service.get(id);
    }

    @PostMapping(consumes = MediaType.APPLICATION_JSON_VALUE, produces = MediaType.APPLICATION_JSON_VALUE)
    @Operation(summary = "Create a custom location relationship type")
    @ConflictApiResponse
    public LocationRelationshipTypeResponse createLocationRelationshipType(
            @RequestBody NewLocationRelationshipTypeRequest input
    ) {
        return this.service.create(input);
    }

    @PutMapping(value = "/{id}", consumes = MediaType.APPLICATION_JSON_VALUE, produces = MediaType.APPLICATION_JSON_VALUE)
    @Operation(summary = "Update a custom location relationship type")
    @NotFoundApiResponse
    public LocationRelationshipTypeResponse updateLocationRelationshipType(
            @PathVariable("id") ResourceIdentifier id,
            @RequestBody UpdateLocationRelationshipTypeRequest input
    ) {
        return this.service.update(id, input);
    }
}
