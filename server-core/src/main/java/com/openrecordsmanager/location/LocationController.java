package com.openrecordsmanager.location;

import com.openrecordsmanager.api.ResourceIdentifier;
import com.openrecordsmanager.location.dto.LocationResponse;
import com.openrecordsmanager.location.dto.LocationSearchRequest;
import com.openrecordsmanager.location.dto.LocationSearchResponse;
import com.openrecordsmanager.location.relationship.RelationshipDirection;
import com.openrecordsmanager.location.relationship.dto.LocationRelationshipResponse;
import com.openrecordsmanager.location.relationship.dto.NewLocationRelationshipRequest;
import com.openrecordsmanager.location.user.User;
import com.openrecordsmanager.rest.swagger.ConflictApiResponse;
import com.openrecordsmanager.rest.swagger.DefaultApiResponses;
import com.openrecordsmanager.rest.swagger.NotFoundApiResponse;
import io.swagger.v3.oas.annotations.Operation;
import org.jspecify.annotations.Nullable;
import org.springframework.http.MediaType;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.web.bind.annotation.*;

import java.util.List;
import java.util.UUID;

@RestController
@RequestMapping("/api/location")
@DefaultApiResponses
@PreAuthorize("isAuthenticated()")
public class LocationController {

    private final LocationService service;

    public LocationController(LocationService service) {
        this.service = service;
    }

    @PostMapping(value = "/search", consumes = MediaType.APPLICATION_JSON_VALUE, produces = MediaType.APPLICATION_JSON_VALUE)
    @Operation(summary = "Search locations by property criteria")
    public LocationSearchResponse searchLocations(@AuthenticationPrincipal User user, @RequestBody LocationSearchRequest input) {
        return this.service.search(user, input);
    }

    @GetMapping(value = "/{id}", produces = MediaType.APPLICATION_JSON_VALUE)
    @Operation(summary = "Get location details")
    @NotFoundApiResponse
    public LocationResponse getLocation(@PathVariable("id") UUID id) {
        return this.service.get(id);
    }

    @GetMapping(value = "/{id}/relationships", produces = MediaType.APPLICATION_JSON_VALUE)
    @Operation(summary = "List active relationships for a location")
    @NotFoundApiResponse
    public List<LocationRelationshipResponse> listLocationRelationships(
            @PathVariable("id") UUID id,
            @RequestParam(value = "direction", defaultValue = "outgoing") RelationshipDirection direction,
            @RequestParam(value = "type", required = false) @Nullable ResourceIdentifier type
    ) {
        return this.service.listRelationships(id, direction, type);
    }

    @PostMapping(
            value = "/{id}/relationships",
            consumes = MediaType.APPLICATION_JSON_VALUE,
            produces = MediaType.APPLICATION_JSON_VALUE
    )
    @Operation(summary = "Create a relationship from this location to another")
    @NotFoundApiResponse
    @ConflictApiResponse
    public LocationRelationshipResponse createLocationRelationship(
            @PathVariable("id") UUID id,
            @RequestBody NewLocationRelationshipRequest input
    ) {
        return this.service.createRelationship(id, input);
    }

    @DeleteMapping(value = "/{id}/relationships/{relationshipId}")
    @Operation(summary = "End an active relationship involving this location")
    @NotFoundApiResponse
    public void endLocationRelationship(
            @PathVariable("id") UUID id,
            @PathVariable("relationshipId") UUID relationshipId
    ) {
        this.service.endRelationship(id, relationshipId);
    }
}
