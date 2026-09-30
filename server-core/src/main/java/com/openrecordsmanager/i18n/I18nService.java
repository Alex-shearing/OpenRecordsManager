package com.openrecordsmanager.i18n;

import com.google.common.collect.ImmutableMap;
import org.apache.commons.lang3.StringUtils;
import org.jspecify.annotations.Nullable;
import org.springframework.context.support.AbstractMessageSource;
import org.springframework.stereotype.Service;

import java.text.MessageFormat;
import java.util.HashSet;
import java.util.Locale;
import java.util.Map;
import java.util.Set;
import java.util.concurrent.ConcurrentHashMap;

/**
 * The primary service for internationalization of string/messages.
 */
@Service("messageSource")
public class I18nService extends AbstractMessageSource {
    private final @Nullable TranslationOverrideService overrideService;
    private final BundleMessageLoader bundleMessageLoader;

    public I18nService(@Nullable TranslationOverrideService overrideService, BundleMessageLoader bundleMessageLoader) {
        this.overrideService = overrideService;
        this.bundleMessageLoader = bundleMessageLoader;
        this.setUseCodeAsDefaultMessage(true);
    }

    private String resolveKeyToFormat(String key, Locale locale) {
        if (this.overrideService != null) {
            String override = this.overrideService.getCached(locale, key);
            if (override != null) {
                return override;
            }
        }

        String bundled = this.bundleMessageLoader.findMessage(locale, key);
        if (bundled != null) {
            return bundled;
        }

        if (!Locale.ENGLISH.equals(locale) && !Locale.ROOT.equals(locale)) {
            return this.resolveKeyToFormat(key, Locale.ENGLISH);
        }

        return key;
    }

    /**
     * Flattened catalog for a locale: every known key resolved with override → bundled precedence.
     */
    public Map<String, String> snapshot(Locale locale) {
        return this.allKeys().stream()
                .map(s -> Map.entry(s, this.resolveKeyToFormat(s, locale)))
                .collect(ImmutableMap.toImmutableMap(Map.Entry::getKey, Map.Entry::getValue));
    }

    private Set<String> allKeys() {
        Set<String> keys = new HashSet<>(this.bundleMessageLoader.getAllKeys());
        if (this.overrideService != null) {
            keys.addAll(this.overrideService.allCachedKeys());
        }
        return keys;
    }

    @Override
    protected MessageFormat resolveCode(String code, Locale locale) {
        String message = this.resolveKeyToFormat(code, locale);
        return this.createMessageFormat(message, locale);
    }

    @Override
    protected String resolveCodeWithoutArguments(String code, Locale locale) {
        return this.resolveKeyToFormat(code, locale);
    }

    static @Nullable String findMessage(
            ConcurrentHashMap<Locale, ConcurrentHashMap<String, String>> source,
            Locale locale,
            String key
    ) {
        ConcurrentHashMap<String, String> localeMap = source.get(locale);

        if (localeMap != null) {
            String cachedValue = localeMap.get(key);
            if (cachedValue != null) {
                return cachedValue;
            }
        }

        String language = locale.getLanguage();
        if (StringUtils.isBlank(language)) {
            return null;
        }

        Locale generalLocale = Locale.forLanguageTag(language);
        if (locale.equals(generalLocale)) {
            return null;
        }

        // Lookup fallback generalized map (e.g., using 'en' instead of 'en_US')
        ConcurrentHashMap<String, String> generalizedLocaleMap = source.get(generalLocale);

        return generalizedLocaleMap != null ? generalizedLocaleMap.get(key) : null;
    }
}
