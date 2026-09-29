package com.openrecordsmanager.i18n;

import jakarta.persistence.Column;
import jakarta.persistence.EmbeddedId;
import jakarta.persistence.Entity;
import jakarta.persistence.Table;
import org.jspecify.annotations.Nullable;

import java.time.Instant;
import java.util.Locale;
import java.util.Objects;

@Entity
@Table(name = "translation_override")
@SuppressWarnings("NotNullFieldNotInitialized")
public class TranslationOverride {

    @EmbeddedId
    private TranslationOverrideId id;

    @Column(nullable = false, length = 4000)
    private String value;

    @Column(name = "updated_at", nullable = false)
    private Instant updatedAt;

    @Deprecated
    protected TranslationOverride() {
    }

    public TranslationOverride(String messageKey, Locale locale, String value) {
        this.id = new TranslationOverrideId(messageKey, locale);
        this.value = value;
        this.updatedAt = Instant.now();
    }

    public TranslationOverrideId getId() {
        return this.id;
    }

    public String getMessageKey() {
        return this.id.getMessageKey();
    }

    public Locale getLocale() {
        return this.id.getLocale();
    }

    public String getValue() {
        return this.value;
    }

    public void setValue(String value) {
        this.value = value;
        this.updatedAt = Instant.now();
    }

    public Instant getUpdatedAt() {
        return this.updatedAt;
    }

    @Override
    public boolean equals(@Nullable Object o) {
        if (this == o) {
            return true;
        }
        if (!(o instanceof TranslationOverride that)) {
            return false;
        }
        return Objects.equals(this.id, that.id);
    }

    @Override
    public int hashCode() {
        return Objects.hashCode(this.id);
    }
}
