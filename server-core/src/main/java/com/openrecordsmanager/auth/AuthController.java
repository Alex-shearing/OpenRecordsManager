package com.openrecordsmanager.auth;

import com.openrecordsmanager.api.ComponentReference;
import com.openrecordsmanager.auth.dto.AuthProviderListResponse;
import com.openrecordsmanager.auth.dto.LoginResponse;
import com.openrecordsmanager.auth.dto.NewAuthProviderRequest;
import com.openrecordsmanager.rest.dto.ApiResponseV1;
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

@RestController
@RequestMapping("/api/auth")
@InternalServerErrorApiResponse
@ApiResponse(responseCode = "200")
public class AuthController {

    private final AuthService authService;

    public AuthController(AuthService authService) {
        this.authService = authService;
    }

    @GetMapping(value = "/providers", produces = MediaType.APPLICATION_JSON_VALUE)
    @Operation(summary = "List all supported authentication providers.")
    public Set<AuthProviderListResponse> providers_listAll() {
        return this.authService.listProviders();
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

    @PostMapping(value = "/signup", produces = MediaType.APPLICATION_JSON_VALUE)
    @Operation(summary = "Trigger a signup to an authentication provider, implementations vary depending on the provider.")
    @NotFoundApiResponse
    public UUID signup(@RequestBody Map<String, String> loginRequest) {
        // TODO: wire signup through AuthService
        return null;
    }

    @GetMapping(value = "/redirect/{auth_provider}", produces = MediaType.APPLICATION_JSON_VALUE)
    @Operation(summary = "Trigger a redirect from an authentication provider, implementations vary depending on the provider.")
    @NotFoundApiResponse
    public ResponseEntity<Void> redirect(@PathVariable("auth_provider") UUID authProvider) {
        return ResponseEntity.status(HttpStatus.FOUND)
                .location(this.authService.getRedirectLocation(authProvider))
                .build();
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
        return this.authService.login(
                new PluginAuthenticationProvider.RedirectToken(
                        provider,
                        URI.create(request.getRequestURI())
                ),
                request,
                response
        );
    }
}
