package com.openrecordsmanager.auth;

import com.openrecordsmanager.api.ApiResponseV1;
import com.openrecordsmanager.api.auth.RedirectAuthProviderType;
import com.openrecordsmanager.api.errors.ApiError;
import com.openrecordsmanager.api.swagger.InternalServerErrorApiResponse;
import com.openrecordsmanager.api.swagger.NotFoundApiResponse;
import com.openrecordsmanager.auth.dto.AuthProviderListResponse;
import com.openrecordsmanager.auth.dto.LoginResponse;
import com.openrecordsmanager.auth.entity.AuthProvider;
import com.openrecordsmanager.database.DataRepository;
import com.openrecordsmanager.plugin.ComponentCatalog;
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
                .map(provider -> new AuthProviderListResponse(
                        provider.id,
                        provider.providerType
                ))
                .collect(Collectors.toSet());
    }

    @PostMapping(value = "/login/{provider}", produces = MediaType.APPLICATION_JSON_VALUE)
    @Operation(summary = "Trigger a login to an authentication provider, implementations vary depending on the provider.")
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
    public LoginResponse login(
            @PathVariable("provider") UUID provider,
            @RequestBody Map<String, String> inputs,
            HttpServletRequest request,
            HttpServletResponse response
    ) {
        PluginAuthenticationProvider.InputToken auth = new PluginAuthenticationProvider.InputToken(provider, inputs);
        return this.authService.login(auth, request, response);
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
                .orElseThrow(() -> ApiError.notFound("authentication provider", authProvider.toString()));
        RedirectAuthProviderType type = (RedirectAuthProviderType) provider.providerType.getComponent(this.catalog)
                .orElseThrow(() -> ApiError.serverError("authentication provider type {0} not found", provider.providerType));

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
