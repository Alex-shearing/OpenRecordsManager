package com.openrecordsmanager.controllers;

import com.openrecordsmanager.api.auth.RedirectAuthProviderType;
import com.openrecordsmanager.api.auth.UserDetails;
import com.openrecordsmanager.api.types.ComponentTypes;
import com.openrecordsmanager.controllers.repsonse.InternalServerErrorApiResponse;
import com.openrecordsmanager.controllers.repsonse.NotFoundApiResponse;
import com.openrecordsmanager.controllers.repsonse.errors.ApiError;
import com.openrecordsmanager.controllers.repsonse.errors.ApiResponseWrapper;
import com.openrecordsmanager.model.AuthProvider;
import com.openrecordsmanager.model.repositories.DataRepository;
import com.openrecordsmanager.resources.ComponentCatalog;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.media.Content;
import io.swagger.v3.oas.annotations.media.ExampleObject;
import io.swagger.v3.oas.annotations.media.Schema;
import io.swagger.v3.oas.annotations.responses.ApiResponse;
import jakarta.servlet.http.HttpServletRequest;
import org.springframework.http.HttpStatus;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.servlet.support.ServletUriComponentsBuilder;

import java.net.URI;
import java.net.URISyntaxException;
import java.util.Map;
import java.util.UUID;

@RestController
@RequestMapping("/api/auth")
@InternalServerErrorApiResponse
@ApiResponse(responseCode = "200")
public class AuthController {

    private final ComponentCatalog catalog;
    private final DataRepository repository;

    public AuthController(ComponentCatalog catalog, DataRepository repository) {
        this.catalog = catalog;
        this.repository = repository;
    }

    @PostMapping(value = "/login", produces = MediaType.APPLICATION_JSON_VALUE)
    @Operation(summary = "Trigger a login from an authentication provider, implementations vary depending on the provider.")
    @NotFoundApiResponse
    @ApiResponse(
            responseCode = "401",
            description = "Authentication Failed",
            content = @Content(
                    mediaType = MediaType.APPLICATION_JSON_VALUE,
                    schema = @Schema(implementation = ApiResponseWrapper.class),
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
    public String login(@RequestBody Map<String, String> loginRequest) {
        String username = loginRequest.get("username");
        String password = loginRequest.get("password");
        String requestedProvider = loginRequest.get("provider"); // e.g., "LDAP"

        return "ok";
    }

    @GetMapping(value = "/redirect/{auth_provider}", produces = MediaType.APPLICATION_JSON_VALUE)
    @Operation(summary = "Trigger a redirect from an authentication provider, implementations vary depending on the provider.")
    @NotFoundApiResponse
    public ResponseEntity<Void> redirect(@PathVariable("auth_provider") UUID authProvider) {
        AuthProvider provider = this.repository.authProviderRepo.findById(authProvider)
                .orElseThrow(() -> ApiError.notFound("authentication provider", authProvider.toString()));
        RedirectAuthProviderType type = this.catalog.getComponent(ComponentTypes.REDIRECT_AUTH_PROVIDER, provider.providerType)
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
                    schema = @Schema(implementation = ApiResponseWrapper.class),
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
    public AuthenticationResponse callback(HttpServletRequest request, @PathVariable("auth_provider") UUID authProvider) throws URISyntaxException {
        AuthProvider provider = this.repository.authProviderRepo.findById(authProvider)
                .orElseThrow(() -> ApiError.notFound("authentication provider", authProvider.toString()));
        RedirectAuthProviderType type = this.catalog.getComponent(ComponentTypes.REDIRECT_AUTH_PROVIDER, provider.providerType)
                .orElseThrow(() -> ApiError.serverError("authentication provider type {0} not found", provider.providerType));

        URI fullUri = new URI(ServletUriComponentsBuilder.fromRequest(request).toUriString());
        UserDetails user = type.authenticateCallback(provider, fullUri);
        if (user == null) {
            throw ApiError.authError("Username or password is incorrect");
        }

        return new AuthenticationResponse("token");
    }

    public record AuthenticationResponse(String token) {
    }
}
