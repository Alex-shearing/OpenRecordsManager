package com.openrecordsmanager.auth;

import com.openrecordsmanager.auth.dto.*;
import com.openrecordsmanager.rest.dto.ApiResponseV1;
import com.openrecordsmanager.rest.swagger.*;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.media.Content;
import io.swagger.v3.oas.annotations.media.ExampleObject;
import io.swagger.v3.oas.annotations.media.Schema;
import io.swagger.v3.oas.annotations.responses.ApiResponse;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.http.HttpStatus;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.*;

import java.net.URI;
import java.util.Map;
import java.util.Set;
import java.util.UUID;

@RestController
@RequestMapping("/api/auth")
@InternalServerErrorApiResponse
@ApiResponse(responseCode = "200")
public class AuthController {
    private static final Logger LOGGER = LoggerFactory.getLogger(AuthController.class);

    private final AuthService authService;

    public AuthController(AuthService authService) {
        this.authService = authService;
    }

    @GetMapping(value = "/available_providers", produces = MediaType.APPLICATION_JSON_VALUE)
    @Operation(summary = "List available authentication providers.")
    public Set<SimpleAuthProviderResponse> retrieveAvailableAuthProviders() {
        return this.authService.listProviders();
    }

    @GetMapping(value = "/providers", produces = MediaType.APPLICATION_JSON_VALUE)
    @Operation(summary = "List all configured authentication providers.")
    @PreAuthorize("isAuthenticated()")
    @UnauthorizedApiResponse
    @ForbiddenApiResponse
    public Set<AuthProviderResponse> retrieveAllAuthProviders() {
        return this.authService.listAllProviders();
    }

    @GetMapping(value = "/providers/types", produces = MediaType.APPLICATION_JSON_VALUE)
    @Operation(summary = "List installed authentication provider types and their settings schemas.")
    @PreAuthorize("isAuthenticated()")
    @UnauthorizedApiResponse
    @ForbiddenApiResponse
    public AuthProviderTypeResponse[] retrieveAuthProviderTypes() {
        return this.authService.listProviderTypes();
    }

    @GetMapping(value = "/providers/{id}", produces = MediaType.APPLICATION_JSON_VALUE)
    @Operation(summary = "Get authentication provider details including settings.")
    @PreAuthorize("isAuthenticated()")
    @UnauthorizedApiResponse
    @ForbiddenApiResponse
    @NotFoundApiResponse
    public AuthProviderResponse getAuthProvider(@PathVariable("id") UUID id) {
        return this.authService.getProvider(id);
    }

    @PutMapping(value = "/providers", produces = MediaType.APPLICATION_JSON_VALUE)
    @Operation(summary = "Create new authentication provider")
    @PreAuthorize("isAuthenticated()")
    @ForbiddenApiResponse
    @UnauthorizedApiResponse
    public AuthProviderResponse createAuthProvider(@RequestBody NewAuthProviderRequest provider) {
        return this.authService.createProvider(
                provider.name(),
                provider.type().toReference(),
                provider.settings()
        );
    }

    @PutMapping(value = "/providers/{id}", consumes = MediaType.APPLICATION_JSON_VALUE, produces = MediaType.APPLICATION_JSON_VALUE)
    @Operation(summary = "Update an authentication provider")
    @PreAuthorize("isAuthenticated()")
    @ForbiddenApiResponse
    @UnauthorizedApiResponse
    @NotFoundApiResponse
    public AuthProviderResponse updateAuthProvider(
            @PathVariable("id") UUID id,
            @RequestBody UpdateAuthProviderRequest input
    ) {
        return this.authService.updateProvider(id, input);
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
        return this.authService.login(
                new PluginAuthenticationProvider.InputToken(provider, inputs),
                request,
                response
        );
    }

    @PostMapping(value = "/logout", produces = MediaType.APPLICATION_JSON_VALUE)
    @Operation(summary = "Log out and invalidate the current session token")
    public void logout(HttpServletRequest request, HttpServletResponse response) {
        this.authService.logout(request, response);
    }

    @PostMapping(value = "/refresh", produces = MediaType.APPLICATION_JSON_VALUE)
    @Operation(summary = "Exchange a refresh token for a new access and refresh token pair")
    @ApiResponse(
            responseCode = "401",
            description = "Refresh Failed",
            content = @Content(
                    mediaType = MediaType.APPLICATION_JSON_VALUE,
                    schema = @Schema(implementation = ApiResponseV1.class),
                    examples = @ExampleObject(
                            value = """
                                    {
                                      "success": false,
                                      "errorCode": "Invalid or expired token",
                                      "timestamp": "2026-06-29T23:05:00Z"
                                    }
                                    """
                    )
            )
    )
    public LoginResponse refresh(HttpServletRequest request, HttpServletResponse response) {
        return this.authService.refresh(request, response);
    }

    @PostMapping(value = "/signup", produces = MediaType.APPLICATION_JSON_VALUE)
    @Operation(summary = "Trigger a signup to an authentication provider, implementations vary depending on the provider.")
    @NotFoundApiResponse
    public UUID signup(@RequestBody Map<String, String> loginRequest) {
        // TODO: wire signup through AuthService
        return UUID.randomUUID();
    }

    @GetMapping(value = "/redirect/{auth_provider}")
    @Operation(summary = "Start a redirect authentication flow and send the browser to the identity provider.")
    @NotFoundApiResponse
    public ResponseEntity<Void> redirect(
            @PathVariable("auth_provider") UUID authProvider,
            @RequestParam(value = "redirect", required = false) String redirect,
            HttpServletResponse response
    ) {
        URI location = this.authService.beginRedirectLogin(authProvider, redirect, response);
        return ResponseEntity.status(HttpStatus.FOUND)
                .location(location)
                .build();
    }

    @GetMapping(value = "/callback/{auth_provider}")
    @Operation(summary = "Complete a redirect authentication callback from an identity provider.")
    @NotFoundApiResponse
    public ResponseEntity<Void> callback(
            @PathVariable("auth_provider") UUID provider,
            HttpServletRequest request,
            HttpServletResponse response
    ) {
        try {
            URI returnTo = this.authService.completeRedirectLogin(provider, request, response);
            return ResponseEntity.status(HttpStatus.FOUND)
                    .location(returnTo)
                    .build();
        } catch (Exception e) {
            LOGGER.warn("Redirect authentication callback failed for provider {}: {}", provider, e.getMessage(), e);

            this.authService.clearAuthCookies(response);

            return ResponseEntity.status(HttpStatus.FOUND)
                    .location(URI.create("/login?error=auth_failed"))
                    .build();
        }
    }
}
