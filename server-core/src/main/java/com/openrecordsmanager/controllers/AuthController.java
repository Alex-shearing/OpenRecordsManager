package com.openrecordsmanager.controllers;

import com.openrecordsmanager.api.auth.RedirectAuthProviderType;
import com.openrecordsmanager.api.auth.UserDetails;
import com.openrecordsmanager.controllers.errors.ApiError;
import com.openrecordsmanager.model.AuthProvider;
import com.openrecordsmanager.model.repositories.DataRepository;
import com.openrecordsmanager.resources.ComponentCatalog;
import com.openrecordsmanager.resources.types.ComponentTypes;
import jakarta.servlet.http.HttpServletRequest;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.servlet.support.ServletUriComponentsBuilder;

import java.net.URI;
import java.net.URISyntaxException;
import java.util.Map;
import java.util.UUID;

@RestController
@RequestMapping("/auth")
public class AuthController {

    private final ComponentCatalog catalog;
    private final DataRepository repository;

    public AuthController(ComponentCatalog catalog, DataRepository repository) {
        this.catalog = catalog;
        this.repository = repository;
    }

    @PostMapping("/login")
    public String login(@RequestBody Map<String, String> loginRequest) {
        String username = loginRequest.get("username");
        String password = loginRequest.get("password");
        String requestedProvider = loginRequest.get("provider"); // e.g., "LDAP"

        return "ok";
    }

    @GetMapping("/redirect/{auth_provider}")
    public ResponseEntity<Void> redirect(@PathVariable("auth_provider") UUID authProvider) {
        AuthProvider provider = this.repository.authProviderRepo.findById(authProvider)
                .orElseThrow(() -> ApiError.serverError("authentication provider not found"));
        RedirectAuthProviderType type = ComponentTypes.REDIRECT_AUTH_PROVIDER.getComponent(provider.providerType, this.catalog)
                .orElseThrow(() -> ApiError.serverError("authentication provider type {0} not found", provider.providerType));

        return ResponseEntity.status(HttpStatus.FOUND).location(type.getRedirectTo(provider)).build();
    }


    @GetMapping("/callback/{auth_provider}")
    public ApiResponse<AuthenticationResponse> callback(HttpServletRequest request, @PathVariable("auth_provider") UUID authProvider) throws URISyntaxException {
        AuthProvider provider = this.repository.authProviderRepo.findById(authProvider)
                .orElseThrow(() -> ApiError.serverError("authentication provider not found"));
        RedirectAuthProviderType type = ComponentTypes.REDIRECT_AUTH_PROVIDER.getComponent(provider.providerType, this.catalog)
                .orElseThrow(() -> ApiError.serverError("authentication provider type {0} not found", provider.providerType));

        URI fullUri = new URI(ServletUriComponentsBuilder.fromRequest(request).toUriString());
        UserDetails user = type.authenticateCallback(provider, fullUri);
        if (user == null) {
            throw ApiError.authError("Username or password is incorrect");
        }

        return ApiResponse.success(new AuthenticationResponse("token"));
    }

    public record AuthenticationResponse(String token) {
    }
}
