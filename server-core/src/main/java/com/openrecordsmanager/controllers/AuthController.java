package com.openrecordsmanager.controllers;

import com.openrecordsmanager.api.auth.RedirectAuthProviderType;
import com.openrecordsmanager.api.auth.UserDetails;
import com.openrecordsmanager.model.AuthProvider;
import com.openrecordsmanager.model.repositories.DataRepository;
import com.openrecordsmanager.resources.ResourceCatalog;
import com.openrecordsmanager.resources.types.ResourceTypes;
import jakarta.servlet.http.HttpServletRequest;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.servlet.support.ServletUriComponentsBuilder;

import java.net.URI;
import java.net.URISyntaxException;
import java.util.Map;
import java.util.Optional;
import java.util.UUID;

@RestController
@RequestMapping("/auth")
public class AuthController {

    private final ResourceCatalog catalog;
    private final DataRepository repository;

    // Spring automatically injects the PluginRuntimeManager Bean here
    public AuthController(ResourceCatalog catalog, DataRepository repository) {
        this.catalog = catalog;
        this.repository = repository;
    }

    @GetMapping("/provider_types")
    public ResponseEntity<String> providerTypes() {
        return ResponseEntity.status(HttpStatus.OK).body("ok");
    }

    @PostMapping("/login")
    public ResponseEntity<String> login(@RequestBody Map<String, String> loginRequest) {
        String username = loginRequest.get("username");
        String password = loginRequest.get("password");
        String requestedProvider = loginRequest.get("provider"); // e.g., "LDAP"

        return ResponseEntity.status(HttpStatus.OK).body("ok");
    }

    @GetMapping("/redirect/{auth_provider}")
    public ResponseEntity<ApiResponse<AuthenticationResponse>> redirect(@PathVariable("auth_provider") UUID authProvider) {
        Optional<AuthProvider> provider = this.repository.authProviderRepo.findById(authProvider);
        if (provider.isEmpty()) {
            return ResponseEntity.badRequest().body(ApiResponse.error("invalid authentication provider"));
        }

        Optional<RedirectAuthProviderType> type = ResourceTypes.REDIRECT_AUTH_PROVIDER.getComponent(provider.get().providerType, this.catalog);
        if (type.isEmpty()) {
            return ResponseEntity.badRequest().body(ApiResponse.error("invalid authentication provider"));
        }

        return ResponseEntity.status(HttpStatus.FOUND).location(type.get().getRedirectTo(provider.get())).build();
    }


    @GetMapping("/callback/{auth_provider}")
    public ResponseEntity<ApiResponse<AuthenticationResponse>> callback(HttpServletRequest request, @PathVariable("auth_provider") UUID authProvider) throws URISyntaxException {
        Optional<AuthProvider> provider = this.repository.authProviderRepo.findById(authProvider);
        if (provider.isEmpty()) {
            return ResponseEntity.badRequest().body(ApiResponse.error("invalid authentication provider"));
        }

        Optional<RedirectAuthProviderType> type = ResourceTypes.REDIRECT_AUTH_PROVIDER.getComponent(provider.get().providerType, this.catalog);
        if (type.isEmpty()) {
            return ResponseEntity.badRequest().body(ApiResponse.error("invalid authentication provider"));
        }

        URI fullUri = new URI(ServletUriComponentsBuilder.fromRequest(request).toUriString());
        UserDetails user = type.get().authenticateCallback(provider.get(), fullUri);
        if (user == null) {
            return ResponseEntity.badRequest().body(ApiResponse.error("invalid authentication attempt"));
        }

        System.out.println(user.email());

        return ResponseEntity.ok(ApiResponse.success(new AuthenticationResponse("token")));
    }

    public record AuthenticationResponse(String token) {
    }
}
