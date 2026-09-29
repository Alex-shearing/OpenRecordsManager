package com.openrecordsmanager.i18n.dto;

import com.openrecordsmanager.i18n.TranslationOverride;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;

import java.time.Instant;

public record TranslationOverrideResponse(
        @NotBlank String messageKey,
        @NotBlank String locale,
        @NotBlank String value,
        @NotNull Instant updatedAt
) {
    public static TranslationOverrideResponse of(TranslationOverride entity) {
        return new TranslationOverrideResponse(
                entity.getMessageKey(),
                entity.getLocale().toLanguageTag(),
                entity.getValue(),
                entity.getUpdatedAt()
        );
    }
}
