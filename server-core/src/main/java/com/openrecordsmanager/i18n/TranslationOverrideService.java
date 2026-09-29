package com.openrecordsmanager.i18n;

import com.openrecordsmanager.api.audit.AuditEntityType;
import com.openrecordsmanager.api.audit.AuditOperation;
import com.openrecordsmanager.audit.AuditService;
import com.openrecordsmanager.audit.RequiresAuditComment;
import com.openrecordsmanager.i18n.dto.TranslationOverrideRequest;
import com.openrecordsmanager.i18n.dto.TranslationOverrideResponse;
import com.openrecordsmanager.rest.errors.ResourceNotFoundException;
import jakarta.annotation.PostConstruct;
import org.jspecify.annotations.Nullable;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;
import java.util.Locale;
import java.util.Optional;
import java.util.Set;
import java.util.concurrent.ConcurrentHashMap;
import java.util.stream.Collectors;

@Service
public class TranslationOverrideService {

    private final TranslationOverrideRepository repository;
    private final AuditService auditService;

    private final ConcurrentHashMap<Locale, ConcurrentHashMap<String, String>> cached = new ConcurrentHashMap<>();

    public TranslationOverrideService(TranslationOverrideRepository repository, AuditService auditService) {
        this.repository = repository;
        this.auditService = auditService;
    }

    @PostConstruct
    void warmCache() {
        this.cached.clear();
        for (TranslationOverride override : this.repository.findAll()) {
            this.cacheOverride(override.getLocale(), override.getMessageKey(), override.getValue());
        }
    }

    private void cacheOverride(Locale locale, String key, String value) {
        this.cached.computeIfAbsent(locale, ignored -> new ConcurrentHashMap<>()).put(key, value);
    }

    @Nullable String getCached(Locale locale, String key) {
        return I18nService.findMessage(this.cached, locale, key);
    }

    Set<String> allCachedKeys() {
        return this.cached.values().stream()
                .flatMap(map -> map.keySet().stream())
                .collect(Collectors.toSet());
    }

    @Transactional(readOnly = true)
    public List<TranslationOverrideResponse> list(@Nullable String prefix, @Nullable Locale locale) {
        List<TranslationOverride> results;

        boolean hasPrefix = prefix != null && !prefix.isBlank();
        if (hasPrefix && locale != null) {
            results = this.repository.findByIdMessageKeyStartingWithAndIdLocale(prefix, locale.toLanguageTag());
        } else if (hasPrefix) {
            results = this.repository.findByIdMessageKeyStartingWith(prefix);
        } else if (locale != null) {
            results = this.repository.findByIdLocale(locale.toLanguageTag());
        } else {
            results = this.repository.findAll();
        }

        return results.stream()
                .map(TranslationOverrideResponse::of)
                .toList();
    }

    @Transactional(readOnly = true)
    public TranslationOverrideResponse get(String messageKey, Locale locale) {
        TranslationOverrideId id = new TranslationOverrideId(messageKey, locale);
        return this.repository.findById(id)
                .map(TranslationOverrideResponse::of)
                .orElseThrow(() -> new ResourceNotFoundException("translation_override", id.toString()));
    }

    @Transactional
    @RequiresAuditComment(operation = AuditOperation.UPDATE, targetType = AuditEntityType.CONFIG)
    public TranslationOverrideResponse upsert(TranslationOverrideRequest request) {
        Locale locale = request.getLocale();

        TranslationOverrideId id = new TranslationOverrideId(request.messageKey(), locale);
        TranslationOverride entity = this.repository.findById(id)
                .orElseGet(() -> new TranslationOverride(request.messageKey(), locale, request.value()));

        entity.setValue(request.value());
        this.repository.saveAndFlush(entity);

        this.cacheOverride(locale, entity.getMessageKey(), entity.getValue());

        this.auditService.addEvent(AuditOperation.UPDATE, AuditEntityType.CONFIG, id.toString());
        return TranslationOverrideResponse.of(entity);
    }

    /**
     * Upserts an English default for custom catalog items without requiring an audit comment
     * (used from create/update of user-defined properties/lists).
     *
     * @return the previous resolved value for this key and locale (falls back to the key itself)
     */
    @Transactional
    public @Nullable String upsertSilent(String messageKey, Locale locale, String value) {
        Optional<TranslationOverride> optionalOverride = this.repository
                .findById(new TranslationOverrideId(messageKey, locale));

        String previous = optionalOverride.map(TranslationOverride::getValue).orElse(null);

        TranslationOverride entity = optionalOverride
                .orElseGet(() -> new TranslationOverride(messageKey, locale, value));

        entity.setValue(value);
        TranslationOverride saved = this.repository.saveAndFlush(entity);

        this.cacheOverride(locale, saved.getMessageKey(), saved.getValue());

        return previous;
    }

    @Transactional
    @RequiresAuditComment(operation = AuditOperation.DELETE, targetType = AuditEntityType.CONFIG)
    public void delete(String messageKey, Locale locale) {
        TranslationOverrideId id = new TranslationOverrideId(messageKey, locale);
        TranslationOverride entity = this.repository.findById(id)
                .orElseThrow(() -> new ResourceNotFoundException("translation_override", id.toString()));
        this.repository.delete(entity);

        ConcurrentHashMap<String, String> localeMap = this.cached.get(locale);
        if (localeMap != null) {
            localeMap.remove(messageKey);
        }

        this.auditService.addEvent(AuditOperation.DELETE, AuditEntityType.CONFIG, id.toString());
    }
}
