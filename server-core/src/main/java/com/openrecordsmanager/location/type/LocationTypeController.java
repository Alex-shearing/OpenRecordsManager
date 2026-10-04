package com.openrecordsmanager.location.type;

import com.openrecordsmanager.api.ResourceIdentifier;
import com.openrecordsmanager.api.location.LocationKind;
import com.openrecordsmanager.location.type.dto.LocationTypeResponse;
import com.openrecordsmanager.rest.swagger.DefaultApiResponses;
import com.openrecordsmanager.rest.swagger.NotFoundApiResponse;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.Parameter;
import org.jspecify.annotations.Nullable;
import org.springframework.http.MediaType;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

import java.util.List;

@RestController
@RequestMapping("/api/location_types")
@DefaultApiResponses
@PreAuthorize("isAuthenticated()")
public class LocationTypeController {

    private final LocationTypeService service;

    public LocationTypeController(LocationTypeService service) {
        this.service = service;
    }

    @GetMapping(produces = MediaType.APPLICATION_JSON_VALUE)
    @Operation(summary = "List all location types")
    public List<LocationTypeResponse> listLocationTypes(
            @Parameter(description = "Optional location kind filter")
            @RequestParam(value = "kind", required = false) @Nullable LocationKind kind
    ) {
        return this.service.getAll(kind);
    }

    @GetMapping(value = "/{id}", produces = MediaType.APPLICATION_JSON_VALUE)
    @Operation(summary = "Get location type details")
    @NotFoundApiResponse
    public LocationTypeResponse getLocationType(@PathVariable("id") ResourceIdentifier id) {
        return this.service.get(id);
    }
}
