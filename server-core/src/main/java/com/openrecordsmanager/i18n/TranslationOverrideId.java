package com.openrecordsmanager.i18n;

import jakarta.persistence.Column;
import jakarta.persistence.Embeddable;
import org.jspecify.annotations.Nullable;

import java.io.Serializable;
import java.util.Locale;
import java.util.Objects;

@Embeddable
public class TranslationOverrideId implements Serializable {

    @Column(name = "message_key", nullable = false, length = 512)
    private String messageKey;

    @Column(nullable = false, length = 32)
    private String locale;

    @Deprecated
    protected TranslationOverrideId() {
    }

    public TranslationOverrideId(String messageKey, Locale locale) {
        this.messageKey = messageKey;
        this.locale = locale.toLanguageTag();
    }

    public String getMessageKey() {
        return this.messageKey;
    }

    public Locale getLocale() {
        return Locale.forLanguageTag(this.locale);
    }

    @Override
    public boolean equals(@Nullable Object o) {
        if (this == o) {
            return true;
        }
        if (!(o instanceof TranslationOverrideId that)) {
            return false;
        }
        return Objects.equals(this.messageKey, that.messageKey) && Objects.equals(this.locale, that.locale);
    }

    @Override
    public int hashCode() {
        return Objects.hash(this.messageKey, this.locale);
    }

    @Override
    public String toString() {
        return this.messageKey + "@" + this.locale;
    }
}
