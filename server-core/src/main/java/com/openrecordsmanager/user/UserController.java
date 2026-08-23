package com.openrecordsmanager.user;

import com.openrecordsmanager.action.dto.ActionResponse;
import com.openrecordsmanager.api.ResourceIdentifier;
import com.openrecordsmanager.rest.swagger.DefaultApiResponses;
import com.openrecordsmanager.rest.swagger.NotFoundApiResponse;
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

    @GetMapping(value = "/me")
    public UserResponse me(@AuthenticationPrincipal User user) {
        return UserResponse.of(user);
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
