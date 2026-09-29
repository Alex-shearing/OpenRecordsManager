package com.openrecordsmanager.i18n;

import org.jspecify.annotations.Nullable;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.util.List;

@Repository
public interface TranslationOverrideRepository extends JpaRepository<TranslationOverride, TranslationOverrideId> {
    List<TranslationOverride> findByIdMessageKeyStartingWith(String prefix);

    List<TranslationOverride> findByIdLocale(String locale);

    List<TranslationOverride> findByIdMessageKeyStartingWithAndIdLocale(@Nullable String prefix, String languageTag);
}
