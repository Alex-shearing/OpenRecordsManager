package com.openrecordsmanager.auth;

import com.openrecordsmanager.api.ComponentReference;
import com.openrecordsmanager.api.auth.RedirectAuthProviderType;
import com.openrecordsmanager.auth.dto.AuthProviderListResponse;
import com.openrecordsmanager.auth.dto.LoginResponse;
import com.openrecordsmanager.auth.dto.NewAuthProviderRequest;
import com.openrecordsmanager.auth.entity.AuthProvider;
import com.openrecordsmanager.database.DataRepository;
import com.openrecordsmanager.plugin.registry.ComponentCatalog;
import com.openrecordsmanager.rest.ApiResponseV1;
import com.openrecordsmanager.rest.errors.ResourceNotFoundException;
import com.openrecordsmanager.rest.swagger.*;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.media.Content;
import io.swagger.v3.oas.annotations.media.ExampleObject;
import io.swagger.v3.oas.annotations.media.Schema;
import io.swagger.v3.oas.annotations.responses.ApiResponse;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import org.springframework.http.HttpStatus;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.*;

import java.net.URI;
import java.util.Map;
import java.util.Set;
import java.util.UUID;
import java.util.stream.Collectors;

@RestController
@RequestMapping("/api/auth")
@InternalServerErrorApiResponse
@ApiResponse(responseCode = "200")
public class AuthController {

    private final ComponentCatalog catalog;
    private final DataRepository repository;
    private final AuthService authService;

    public AuthController(ComponentCatalog catalog, DataRepository repository, AuthService authService) {
        this.catalog = catalog;
        this.repository = repository;
        this.authService = authService;
    }

    @GetMapping(value = "/providers", produces = MediaType.APPLICATION_JSON_VALUE)
    @Operation(summary = "List all supported authentication providers.")
    public Set<AuthProviderListResponse> providers_listAll() {
        return this.repository.authProviderRepo.findAll().stream()
                .map(provider -> AuthProviderListResponse.of(this.catalog, provider))
                .collect(Collectors.toSet());
    }

    @PutMapping(value = "/providers", produces = MediaType.APPLICATION_JSON_VALUE)
    @Operation(summary = "Create new authentication provider")
    @PreAuthorize("isAuthenticated()")
    @ForbiddenApiResponse
    @UnauthorizedApiResponse
    public AuthProviderListResponse createProvider(@RequestBody NewAuthProviderRequest provider) {
        return this.authService.createProvider(
                provider.name(),
                ComponentReference.of(provider.type().type, provider.typeId()),
                provider.settings()
        );
    }

    @PostMapping(value = "/login/{provider}", produces = MediaType.APPLICATION_JSON_VALUE)
    @Operation(summary = "Trigger a login to an authentication provider, implementations vary depending on the provider.")
    @NotFoundApiResponse
    @ValidationFailedApiResponse
    @ApiResponse(
            responseCode = "401",
            description = "Authentication Failed",
            content = @Content(
                    mediaType = MediaType.APPLICATION_JSON_VALUE,
                    schema = @Schema(implementation = ApiResponseV1.class),
                    examples = @ExampleObject(
                            value = """
                                    {
                                      "success": false,
                                      "errorCode": "Username or password is incorrect",
                                      "timestamp": "2026-06-29T23:05:00Z"
                                    }
                                    """
                    )
            )
    )
    public LoginResponse login(
            @PathVariable("provider") UUID provider,
            @RequestBody Map<String, String> inputs,
            HttpServletRequest request,
            HttpServletResponse response
    ) {
        return this.authService.login(new PluginAuthenticationProvider.InputToken(provider, inputs), request, response);
    }

    @PostMapping(value = "/signup", produces = MediaType.APPLICATION_JSON_VALUE)
    @Operation(summary = "Trigger a signup to an authentication provider, implementations vary depending on the provider.")
    @NotFoundApiResponse
    public UUID signup(@RequestBody Map<String, String> loginRequest) {
        String username = loginRequest.get("username");
        String password = loginRequest.get("password");

//        User authenticatedUser = this.authService.signup(username, password);

        return null;
    }

    @GetMapping(value = "/redirect/{auth_provider}", produces = MediaType.APPLICATION_JSON_VALUE)
    @Operation(summary = "Trigger a redirect from an authentication provider, implementations vary depending on the provider.")
    @NotFoundApiResponse
    public ResponseEntity<Void> redirect(@PathVariable("auth_provider") UUID authProvider) {
        AuthProvider provider = this.repository.authProviderRepo.findById(authProvider)
                .orElseThrow(() -> new ResourceNotFoundException("authentication provider", authProvider.toString()));
        RedirectAuthProviderType type = provider.getProviderType(this.catalog, RedirectAuthProviderType.class);

        return ResponseEntity.status(HttpStatus.FOUND).location(type.getRedirectTo(provider)).build();
    }


    @GetMapping(value = "/callback/{auth_provider}", produces = MediaType.APPLICATION_JSON_VALUE)
    @Operation(summary = "Trigger a callback authentication provider, implementations vary depending on the provider.")
    @NotFoundApiResponse
    @ApiResponse(
            responseCode = "401",
            description = "Authentication Failed",
            content = @Content(
                    mediaType = MediaType.APPLICATION_JSON_VALUE,
                    schema = @Schema(implementation = ApiResponseV1.class),
                    examples = @ExampleObject(
                            value = """
                                    {
                                      "success": false,
                                      "errorCode": "Username or password is incorrect",
                                      "timestamp": "2026-06-29T23:05:00Z"
                                    }
                                    """
                    )
            )
    )
    public LoginResponse callback(
            @PathVariable("auth_provider") UUID provider,
            HttpServletRequest request,
            HttpServletResponse response
    ) {
        PluginAuthenticationProvider.AbstractPluginToken auth = new PluginAuthenticationProvider.RedirectToken(provider, URI.create(request.getRequestURI()));
        return this.authService.login(auth, request, response);
    }
}
