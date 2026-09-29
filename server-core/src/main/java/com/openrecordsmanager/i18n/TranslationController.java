package com.openrecordsmanager.i18n;

import com.openrecordsmanager.i18n.dto.TranslationCatalogResponse;
import com.openrecordsmanager.i18n.dto.TranslationOverrideRequest;
import com.openrecordsmanager.i18n.dto.TranslationOverrideResponse;
import com.openrecordsmanager.rest.swagger.AuditCommentRequiredApiResponse;
import com.openrecordsmanager.rest.swagger.ForbiddenApiResponse;
import com.openrecordsmanager.rest.swagger.InternalServerErrorApiResponse;
import com.openrecordsmanager.rest.swagger.NotFoundApiResponse;
import com.openrecordsmanager.rest.swagger.UnauthorizedApiResponse;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.responses.ApiResponse;
import jakarta.validation.Valid;
import org.jspecify.annotations.Nullable;
import org.springframework.context.i18n.LocaleContextHolder;
import org.springframework.http.MediaType;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.*;

import java.util.List;
import java.util.Locale;

@RestController
@RequestMapping("/api/translations")
@InternalServerErrorApiResponse
@ApiResponse(responseCode = "200")
public class TranslationController {

    private final TranslationOverrideService overrideService;
    private final I18nService messageService;

    public TranslationController(
            TranslationOverrideService overrideService,
            I18nService messageService
    ) {
        this.overrideService = overrideService;
        this.messageService = messageService;
    }

    @GetMapping(value = "/catalog", produces = MediaType.APPLICATION_JSON_VALUE)
    @Operation(summary = "Get the resolved translation catalog for the request locale")
    public TranslationCatalogResponse getTranslationCatalog() {
        Locale locale = LocaleContextHolder.getLocale();

        return new TranslationCatalogResponse(
                locale.toLanguageTag(),
                this.messageService.snapshot(locale)
        );
    }

    @GetMapping(value = "/overrides", produces = MediaType.APPLICATION_JSON_VALUE)
    @Operation(summary = "List translation overrides")
    @PreAuthorize("isAuthenticated()")
    @UnauthorizedApiResponse
    @ForbiddenApiResponse
    public List<TranslationOverrideResponse> listTranslationOverrides(
            @RequestParam(required = false) @Nullable String prefix,
            @RequestParam(required = false) @Nullable String locale
    ) {
        Locale parsed = locale != null ? Locale.forLanguageTag(locale) : null;
        return this.overrideService.list(prefix, parsed);
    }

    @GetMapping(value = "/overrides/{messageKey}/{locale}", produces = MediaType.APPLICATION_JSON_VALUE)
    @Operation(summary = "Get a translation override")
    @PreAuthorize("isAuthenticated()")
    @UnauthorizedApiResponse
    @ForbiddenApiResponse
    @NotFoundApiResponse
    public TranslationOverrideResponse getTranslationOverride(
            @PathVariable("messageKey") String messageKey,
            @PathVariable("locale") String locale
    ) {
        return this.overrideService.get(messageKey, Locale.forLanguageTag(locale));
    }

    @PutMapping(
            value = "/overrides",
            consumes = MediaType.APPLICATION_JSON_VALUE,
            produces = MediaType.APPLICATION_JSON_VALUE
    )
    @Operation(summary = "Create or update a translation override")
    @PreAuthorize("isAuthenticated()")
    @UnauthorizedApiResponse
    @ForbiddenApiResponse
    @AuditCommentRequiredApiResponse
    public TranslationOverrideResponse upsertTranslationOverride(
            @Valid @RequestBody TranslationOverrideRequest request
    ) {
        return this.overrideService.upsert(request);
    }

    @DeleteMapping(value = "/overrides/{messageKey}/{locale}", produces = MediaType.APPLICATION_JSON_VALUE)
    @Operation(summary = "Delete a translation override")
    @PreAuthorize("isAuthenticated()")
    @UnauthorizedApiResponse
    @ForbiddenApiResponse
    @NotFoundApiResponse
    @AuditCommentRequiredApiResponse
    public void deleteTranslationOverride(
            @PathVariable("messageKey") String messageKey,
            @PathVariable("locale") String locale
    ) {
        this.overrideService.delete(messageKey, Locale.forLanguageTag(locale));
    }
}
