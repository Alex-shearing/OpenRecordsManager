package com.openrecordsmanager.i18n.dto;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;

import java.util.Map;

public record TranslationCatalogResponse(
        @NotBlank String locale,
        @NotNull Map<String, String> messages
) {
}
