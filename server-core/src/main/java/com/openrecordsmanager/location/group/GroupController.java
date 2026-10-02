package com.openrecordsmanager.location.group;

import com.openrecordsmanager.location.group.dto.*;
import com.openrecordsmanager.location.user.User;
import com.openrecordsmanager.rest.swagger.DefaultApiResponses;
import com.openrecordsmanager.rest.swagger.NotFoundApiResponse;
import io.swagger.v3.oas.annotations.Operation;
import org.springframework.http.MediaType;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.web.bind.annotation.*;

import java.util.UUID;

@RestController
@RequestMapping("/api/group")
@DefaultApiResponses
@PreAuthorize("isAuthenticated()")
public class GroupController {

    private final GroupService service;

    public GroupController(GroupService service) {
        this.service = service;
    }

    @PostMapping(value = "/search", consumes = MediaType.APPLICATION_JSON_VALUE, produces = MediaType.APPLICATION_JSON_VALUE)
    @Operation(summary = "Search groups by property criteria")
    public GroupSearchResponse searchGroups(@AuthenticationPrincipal User user, @RequestBody GroupSearchRequest input) {
        return this.service.search(user, input);
    }

    @GetMapping(value = "/{id}", produces = MediaType.APPLICATION_JSON_VALUE)
    @Operation(summary = "Get group details")
    @NotFoundApiResponse
    public GroupResponse getGroup(@PathVariable("id") UUID id) {
        return this.service.get(id);
    }

    @PostMapping(produces = MediaType.APPLICATION_JSON_VALUE)
    @Operation(summary = "Create a new group")
    public GroupResponse createGroup(@RequestBody NewGroupRequest input) {
        return this.service.create(input);
    }

    @PutMapping(value = "/{id}", consumes = MediaType.APPLICATION_JSON_VALUE, produces = MediaType.APPLICATION_JSON_VALUE)
    @Operation(summary = "Update a group")
    @NotFoundApiResponse
    public GroupResponse updateGroup(@PathVariable("id") UUID id, @RequestBody UpdateGroupRequest input) {
        return this.service.update(id, input);
    }
}
