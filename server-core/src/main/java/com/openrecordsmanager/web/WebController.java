package com.openrecordsmanager.web;

import com.openrecordsmanager.api.builtin.BuiltinConfigs;
import com.openrecordsmanager.config.ConfigService;
import com.openrecordsmanager.rest.swagger.InternalServerErrorApiResponse;
import com.openrecordsmanager.web.dto.WebBrandingResponse;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.responses.ApiResponse;
import org.springframework.http.MediaType;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

/**
 * Public web-client settings. Branding is centralised via {@link ConfigService}.
 */
@RestController
@RequestMapping("/api/web")
@InternalServerErrorApiResponse
@ApiResponse(responseCode = "200")
public class WebController {

    private final ConfigService config;

    public WebController(ConfigService config) {
        this.config = config;
    }

    @GetMapping(produces = MediaType.APPLICATION_JSON_VALUE)
    @Operation(summary = "Get centralised web UI branding")
    public WebBrandingResponse branding() {
        return new WebBrandingResponse(
                this.config.getOrThrow(BuiltinConfigs.WEB_PRODUCT_NAME),
                this.config.getOrThrow(BuiltinConfigs.WEB_LOGO_URL),
                this.config.getOrThrow(BuiltinConfigs.WEB_FAVICON_URL),
                this.config.getOrThrow(BuiltinConfigs.WEB_PRIMARY_COLOR),
                this.config.getOrThrow(BuiltinConfigs.WEB_SUPPORT_URL)
        );
    }
}
