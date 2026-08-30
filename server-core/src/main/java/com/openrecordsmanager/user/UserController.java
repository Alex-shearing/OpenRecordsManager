package com.openrecordsmanager.user;

import com.openrecordsmanager.api.ResourceIdentifier;
import com.openrecordsmanager.rest.dto.ActionResponse;
import com.openrecordsmanager.rest.swagger.ConflictApiResponse;
import com.openrecordsmanager.rest.swagger.DefaultApiResponses;
import com.openrecordsmanager.rest.swagger.NotFoundApiResponse;
import com.openrecordsmanager.user.dto.NewUserRequest;
import com.openrecordsmanager.user.dto.UpdateUserRequest;
import com.openrecordsmanager.user.dto.UserResponse;
import io.swagger.v3.oas.annotations.Operation;
import org.springframework.http.MediaType;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.web.bind.annotation.*;

import java.util.Map;
import java.util.Set;
import java.util.UUID;

@RestController
@RequestMapping("/api/user")
@DefaultApiResponses
@PreAuthorize("isAuthenticated()")
public class UserController {

    private final UserService service;

    public UserController(UserService service) {
        this.service = service;
    }

    @GetMapping(value = "/me", produces = MediaType.APPLICATION_JSON_VALUE)
    @Operation(summary = "Get details of the currently authenticated user")
    public UserResponse me(@AuthenticationPrincipal User user) {
        return this.service.get(user.getId());
    }

    @GetMapping(value = "/{id}", produces = MediaType.APPLICATION_JSON_VALUE)
    @Operation(summary = "Get user details")
    @NotFoundApiResponse
    public UserResponse get(@PathVariable("id") UUID id) {
        return this.service.get(id);
    }

    @PostMapping(produces = MediaType.APPLICATION_JSON_VALUE)
    @Operation(summary = "Create a new user")
    @ConflictApiResponse
    public UserResponse create(@RequestBody NewUserRequest input) {
        return this.service.create(input);
    }

    @PutMapping(value = "/{id}", consumes = MediaType.APPLICATION_JSON_VALUE, produces = MediaType.APPLICATION_JSON_VALUE)
    @Operation(summary = "Update a user")
    @NotFoundApiResponse
    @ConflictApiResponse
    public UserResponse update(@PathVariable("id") UUID id, @RequestBody UpdateUserRequest input) {
        return this.service.update(id, input);
    }

    @GetMapping(value = "/{id}/actions", produces = MediaType.APPLICATION_JSON_VALUE)
    @Operation(summary = "List available actions for a user")
    @NotFoundApiResponse
    public Set<ActionResponse> listActions(@AuthenticationPrincipal User user, @PathVariable("id") UUID id) {
        return this.service.listActions(user, id);
    }

    @PostMapping(value = "/{id}/actions/{action}", consumes = MediaType.APPLICATION_JSON_VALUE)
    @Operation(summary = "Execute an action on a user")
    @NotFoundApiResponse
    public void executeAction(
            @AuthenticationPrincipal User user,
            @PathVariable("id") UUID id,
            @PathVariable("action") ResourceIdentifier action,
            @RequestBody Map<String, ?> inputs
    ) {
        this.service.executeAction(user, id, action, inputs);
    }
}
