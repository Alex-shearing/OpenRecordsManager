package com.openrecordsmanager.controllers;

import com.openrecordsmanager.auth.RedirectAuthProviderType;
import com.openrecordsmanager.auth.UserDetails;
import com.openrecordsmanager.model.AuthProvider;
import com.openrecordsmanager.model.repositories.AuthProviderRepository;
import com.openrecordsmanager.resources.ResourceRegistry;
import jakarta.servlet.http.HttpServletRequest;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.servlet.support.ServletUriComponentsBuilder;

import java.net.URI;
import java.net.URISyntaxException;
import java.util.Map;
import java.util.Optional;

@RestController
@RequestMapping("/auth")
public class AuthController {

    private final ResourceRegistry resource;
    private final AuthProviderRepository authProviderRepository;

    // Spring automatically injects the PluginRuntimeManager Bean here
    public AuthController(ResourceRegistry pluginManager, AuthProviderRepository authProviderRepository) {
        this.resource = pluginManager;
        this.authProviderRepository = authProviderRepository;
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
    public ResponseEntity<ApiResponse<AuthenticationResponse>> redirect(@PathVariable("auth_provider") long authProvider) {
        Optional<AuthProvider> provider = this.authProviderRepository.findById(authProvider);
        if (provider.isEmpty()) {
            return ResponseEntity.badRequest().body(ApiResponse.error("invalid authentication provider"));
        }

        RedirectAuthProviderType type = this.resource.getRedirectAuthProviders().get(provider.get().getProviderType());
        if (type == null) {
            return ResponseEntity.badRequest().body(ApiResponse.error("invalid authentication provider"));
        }

        return ResponseEntity.status(HttpStatus.FOUND).location(type.getRedirectTo(provider.get())).build();
    }


    @GetMapping("/callback/{auth_provider}")
    public ResponseEntity<ApiResponse<AuthenticationResponse>> callback(HttpServletRequest request, @PathVariable("auth_provider") long authProvider) throws URISyntaxException {
        Optional<AuthProvider> provider = this.authProviderRepository.findById(authProvider);
        if (provider.isEmpty()) {
            return ResponseEntity.badRequest().body(ApiResponse.error("invalid authentication provider"));
        }

        RedirectAuthProviderType type = this.resource.getRedirectAuthProviders().get(provider.get().getProviderType());
        if (type == null) {
            return ResponseEntity.badRequest().body(ApiResponse.error("invalid authentication provider"));
        }

        URI fullUri = new URI(ServletUriComponentsBuilder.fromRequest(request).toUriString());
        UserDetails user = type.authenticateCallback(provider.get(), fullUri);
        if (user == null) {
            return ResponseEntity.badRequest().body(ApiResponse.error("invalid authentication attempt"));
        }

        System.out.println(user.email());

        return ResponseEntity.ok(ApiResponse.success(new AuthenticationResponse("token")));
    }

    public record AuthenticationResponse(String token) {
    }
}
