package com.openrecordsmanager.relationship;

import com.openrecordsmanager.api.ResourceIdentifier;
import com.openrecordsmanager.relationship.dto.NewRelationshipTypeRequest;
import com.openrecordsmanager.relationship.dto.RelationshipTypeResponse;
import com.openrecordsmanager.relationship.dto.UpdateRelationshipTypeRequest;
import com.openrecordsmanager.rest.swagger.ConflictApiResponse;
import com.openrecordsmanager.rest.swagger.DefaultApiResponses;
import com.openrecordsmanager.rest.swagger.NotFoundApiResponse;
import com.openrecordsmanager.rest.swagger.ValidationFailedApiResponse;
import io.swagger.v3.oas.annotations.Operation;
import jakarta.validation.Valid;
import org.springframework.http.MediaType;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.*;

import java.util.Set;

@RestController
@RequestMapping("/api/relationship-type")
@DefaultApiResponses
@PreAuthorize("isAuthenticated()")
public class RelationshipTypeController {

    private final RelationshipTypeService service;

    public RelationshipTypeController(RelationshipTypeService service) {
        this.service = service;
    }

    @GetMapping(produces = MediaType.APPLICATION_JSON_VALUE)
    @Operation(summary = "List relationship types")
    public Set<RelationshipTypeResponse> listRelationshipTypes() {
        return this.service.getAll();
    }

    @GetMapping(value = "/{id}", produces = MediaType.APPLICATION_JSON_VALUE)
    @Operation(summary = "Get a relationship type")
    @NotFoundApiResponse
    public RelationshipTypeResponse getRelationshipType(@PathVariable("id") ResourceIdentifier id) {
        return this.service.get(id);
    }

    @PostMapping(consumes = MediaType.APPLICATION_JSON_VALUE, produces = MediaType.APPLICATION_JSON_VALUE)
    @Operation(summary = "Create a custom relationship type")
    @ConflictApiResponse
    @ValidationFailedApiResponse
    public RelationshipTypeResponse createRelationshipType(
            @Valid @RequestBody NewRelationshipTypeRequest input
    ) {
        return this.service.create(input);
    }

    @PutMapping(value = "/{id}", consumes = MediaType.APPLICATION_JSON_VALUE, produces = MediaType.APPLICATION_JSON_VALUE)
    @Operation(summary = "Update a custom relationship type")
    @NotFoundApiResponse
    @ValidationFailedApiResponse
    public RelationshipTypeResponse updateRelationshipType(
            @PathVariable("id") ResourceIdentifier id,
            @Valid @RequestBody UpdateRelationshipTypeRequest input
    ) {
        return this.service.update(id, input);
    }
}
