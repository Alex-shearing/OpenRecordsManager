package com.openrecordsmanager.i18n.dto;

import jakarta.validation.constraints.NotBlank;

import java.util.Locale;

public record TranslationOverrideRequest(
        @NotBlank String messageKey,
        @NotBlank String locale,
        @NotBlank String value
) {
    public Locale getLocale() {
        return Locale.forLanguageTag(this.locale);
    }
}
