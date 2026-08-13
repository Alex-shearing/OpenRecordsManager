package com.openrecordsmanager.user;

import com.openrecordsmanager.api.swagger.DefaultApiResponses;
import com.openrecordsmanager.user.dto.UserResponse;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequestMapping("/api/user")
@DefaultApiResponses
@PreAuthorize("isAuthenticated()")
public class UserController {

    @GetMapping(value = "/me")
    public UserResponse me(@AuthenticationPrincipal User user) {
        return new UserResponse(user.getUsername(), user.toPropertyMap());
    }
}
