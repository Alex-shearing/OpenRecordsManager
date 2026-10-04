package com.openrecordsmanager.location;

import com.openrecordsmanager.api.ResourceIdentifier;
import com.openrecordsmanager.location.dto.LocationResponse;
import com.openrecordsmanager.location.dto.LocationSearchRequest;
import com.openrecordsmanager.location.dto.LocationSearchResponse;
import com.openrecordsmanager.location.dto.NewLocationRequest;
import com.openrecordsmanager.location.dto.UpdateLocationRequest;
import com.openrecordsmanager.location.relationship.RelationshipDirection;
import com.openrecordsmanager.location.relationship.dto.LocationRelationshipResponse;
import com.openrecordsmanager.location.relationship.dto.NewLocationRelationshipRequest;
import com.openrecordsmanager.location.user.User;
import com.openrecordsmanager.rest.dto.ActionResponse;
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
import java.util.Map;
import java.util.Set;
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

    @GetMapping(value = "/me", produces = MediaType.APPLICATION_JSON_VALUE)
    @Operation(summary = "Get details of the currently authenticated user location")
    public LocationResponse me(@AuthenticationPrincipal User user) {
        return this.service.me(user);
    }

    @GetMapping(value = "/{id}", produces = MediaType.APPLICATION_JSON_VALUE)
    @Operation(summary = "Get location details")
    @NotFoundApiResponse
    public LocationResponse getLocation(@PathVariable("id") UUID id) {
        return this.service.get(id);
    }

    @PostMapping(consumes = MediaType.APPLICATION_JSON_VALUE, produces = MediaType.APPLICATION_JSON_VALUE)
    @Operation(summary = "Create a new location (user or group)")
    @ConflictApiResponse
    public LocationResponse createLocation(@RequestBody NewLocationRequest input) {
        return this.service.create(input);
    }

    @PutMapping(value = "/{id}", consumes = MediaType.APPLICATION_JSON_VALUE, produces = MediaType.APPLICATION_JSON_VALUE)
    @Operation(summary = "Update a location (user or group)")
    @NotFoundApiResponse
    @ConflictApiResponse
    public LocationResponse updateLocation(
            @AuthenticationPrincipal User actor,
            @PathVariable("id") UUID id,
            @RequestBody UpdateLocationRequest input
    ) {
        return this.service.update(actor, id, input);
    }

    @GetMapping(value = "/{id}/actions", produces = MediaType.APPLICATION_JSON_VALUE)
    @Operation(summary = "List available actions for a location")
    @NotFoundApiResponse
    public Set<ActionResponse> listLocationActions(@AuthenticationPrincipal User user, @PathVariable("id") UUID id) {
        return this.service.listActions(user, id);
    }

    @PostMapping(value = "/{id}/actions/{action}", consumes = MediaType.APPLICATION_JSON_VALUE)
    @Operation(summary = "Execute an action on a location")
    @NotFoundApiResponse
    public void executeLocationAction(
            @AuthenticationPrincipal User user,
            @PathVariable("id") UUID id,
            @PathVariable("action") ResourceIdentifier action,
            @RequestBody Map<String, ?> inputs
    ) {
        this.service.executeAction(user, id, action, inputs);
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
