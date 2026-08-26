package com.openrecordsmanager.web.dto;

import jakarta.validation.constraints.NotBlank;

public record WebBrandingResponse(
        @NotBlank String productName,
        @NotBlank String logoUrl,
        @NotBlank String faviconUrl,
        @NotBlank String primaryColor,
        @NotBlank String supportUrl
) {
}
