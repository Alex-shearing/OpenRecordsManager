package com.openrecordsmanager.list;

import com.openrecordsmanager.api.ComponentReference;
import com.openrecordsmanager.api.ResourceIdentifier;
import com.openrecordsmanager.api.TranslationField;
import com.openrecordsmanager.api.audit.AuditEntityType;
import com.openrecordsmanager.api.audit.AuditOperation;
import com.openrecordsmanager.api.types.ComponentTypes;
import com.openrecordsmanager.audit.AuditEventDescriptions;
import com.openrecordsmanager.audit.AuditService;
import com.openrecordsmanager.audit.RequiresAuditComment;
import com.openrecordsmanager.database.DataRepository;
import com.openrecordsmanager.i18n.I18nService;
import com.openrecordsmanager.i18n.TranslationOverrideService;
import com.openrecordsmanager.list.dto.*;
import com.openrecordsmanager.rest.exception.ResourceAlreadyExistsException;
import com.openrecordsmanager.rest.exception.ResourceInUseException;
import com.openrecordsmanager.rest.exception.ResourceNotFoundException;
import org.springframework.context.i18n.LocaleContextHolder;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.HashSet;
import java.util.Locale;
import java.util.Set;
import java.util.stream.Collectors;

@Service
public class ListService {

    private final DataRepository repository;
    private final AuditService auditService;
    private final I18nService i18nService;
    private final TranslationOverrideService overrideService;

    public ListService(
            DataRepository repository,
            AuditService auditService,
            I18nService i18nService,
            TranslationOverrideService overrideService
    ) {
        this.repository = repository;
        this.auditService = auditService;
        this.i18nService = i18nService;
        this.overrideService = overrideService;
    }

    @Transactional(readOnly = true)
    public Set<ResourceIdentifier> getAll() {
        Set<ResourceIdentifier> results = this.repository.listTypeRepo.findAll().stream()
                .map(ListType::getId)
                .collect(Collectors.toSet());
        this.auditService.recordCollectionRead(AuditEntityType.LIST, results.size());
        return results;
    }

    @Transactional(readOnly = true)
    public ListTypeResponse get(ResourceIdentifier id) {
        ListType listType = this.repository.listTypeRepo.findById(id)
                .orElseThrow(() -> new ResourceNotFoundException(ComponentTypes.LIST, id));

        this.auditService.addReadEvent(AuditEntityType.LIST, id);
        return ListTypeResponse.of(listType);
    }

    @Transactional
    @RequiresAuditComment(operation = AuditOperation.CREATE, targetType = AuditEntityType.LIST)
    public ListTypeResponse create(NewListTypeRequest input) {
        if (this.repository.listTypeRepo.existsById(input.id())) {
            throw new ResourceAlreadyExistsException(ComponentTypes.LIST, input.id());
        }

        ListType listType = new ListType(input.id());
        this.repository.listTypeRepo.saveAndFlush(listType);
        this.overrideService.upsertSilent(listType.getReference().getTranslationKey(TranslationField.NAME), LocaleContextHolder.getLocale(), input.name());

        this.auditService.addEvent(AuditOperation.CREATE, AuditEntityType.LIST, listType.getId());

        return ListTypeResponse.of(listType);
    }

    @Transactional
    @RequiresAuditComment(operation = AuditOperation.UPDATE, targetType = AuditEntityType.LIST)
    public ListTypeResponse update(ResourceIdentifier id, UpdateListTypeRequest input) {
        ListType listType = this.repository.listTypeRepo.findById(id)
                .orElseThrow(() -> new ResourceNotFoundException(ComponentTypes.LIST, id));
        ComponentReference.Reference<?> ref = listType.getReference();

        String oldName = this.overrideService.upsertSilent(ref.getTranslationKey(TranslationField.NAME), LocaleContextHolder.getLocale(), input.name());
        listType.touchDateModified();
        this.repository.listTypeRepo.saveAndFlush(listType);

        this.auditService.addEvent(
                AuditOperation.UPDATE,
                AuditEntityType.LIST,
                id.toString(),
                AuditEventDescriptions.singleChange("name", oldName, input.name()),
                null,
                null
        );

        return ListTypeResponse.of(listType);
    }

    @Transactional
    @RequiresAuditComment(operation = AuditOperation.DELETE, targetType = AuditEntityType.LIST)
    public void delete(ResourceIdentifier id) {
        ListType listType = this.repository.listTypeRepo.findById(id)
                .orElseThrow(() -> new ResourceNotFoundException(ComponentTypes.LIST, id));

        if (!listType.getChildren().isEmpty()) {
            throw new ResourceInUseException(ComponentTypes.LIST);
        }

        if (this.repository.listTypeRepo.isUsedByObjectProperties(listType)) {
            throw new ResourceInUseException(ComponentTypes.LIST);
        }

        this.repository.listTypeRepo.delete(listType);

        this.auditService.addEvent(AuditOperation.DELETE, AuditEntityType.LIST, id);
    }

    @Transactional(readOnly = true)
    public ListElementResponse getElement(ResourceIdentifier parent, ResourceIdentifier id) {
        ListElement element = this.repository.listElementRepo.getElement(parent, id)
                .orElseThrow(() -> new ResourceNotFoundException(ComponentTypes.LIST_ELEMENT, id));

        this.auditService.addReadEvent(
                AuditEntityType.LIST_ELEMENT,
                id.toString(),
                AuditEventDescriptions.forListElement(element)
        );
        return ListElementResponse.of(element);
    }

    @Transactional(readOnly = true)
    public Set<ListElementResponse> searchElement(ResourceIdentifier parent, String search) {
        ListType type = this.repository.listTypeRepo.findById(parent)
                .orElseThrow(() -> new ResourceNotFoundException(ComponentTypes.LIST, parent));

        String needle = search.toLowerCase(Locale.ROOT);
        Locale locale = LocaleContextHolder.getLocale();

        Set<ListElementResponse> results = type.getChildren().stream()
                .filter(element -> this.matchesElementSearch(element, needle, locale))
                .map(ListElementResponse::of)
                .collect(Collectors.toSet());

        this.auditService.recordSearchRead(AuditEntityType.LIST, parent.toString(), search, results.size());
        return results;
    }

    private boolean matchesElementSearch(ListElement element, String needle, Locale locale) {
        if (needle.isBlank()) {
            return true;
        }
        String key = element.getReference().getTranslationKey(TranslationField.NAME);
        String name = this.i18nService.getMessage(key, new Object[0], locale);

        if (name.toLowerCase(locale).contains(needle)) {
            return true;
        }
        if (element.getId().toString().toLowerCase(locale).contains(needle)) {
            return true;
        }
        return element.getAliases().stream()
                .anyMatch(alias -> alias.toLowerCase(locale).contains(needle));
    }

    @Transactional
    @RequiresAuditComment(operation = AuditOperation.CREATE, targetType = AuditEntityType.LIST_ELEMENT)
    public ListElementResponse createElement(ResourceIdentifier parentId, NewListElementRequest input) {
        ListType parent = this.repository.listTypeRepo.findById(parentId)
                .orElseThrow(() -> new ResourceNotFoundException(ComponentTypes.LIST, parentId));

        if (this.repository.listElementRepo.existsById(input.id())) {
            throw new ResourceAlreadyExistsException(ComponentTypes.LIST_ELEMENT, input.id());
        }

        ListElement element = new ListElement(
                input.id(),
                parent,
                input.index(),
                input.activeTo(),
                new HashSet<>(input.aliases())
        );
        ComponentReference.Reference<?> ref = element.getReference();

        this.repository.listElementRepo.saveAndFlush(element);

        Locale locale = LocaleContextHolder.getLocale();
        this.overrideService.upsertSilent(ref.getTranslationKey(TranslationField.NAME), locale, input.name());
        this.overrideService.upsertSilent(ref.getTranslationKey(TranslationField.DESCRIPTION), locale, input.description());

        this.auditService.addEvent(
                AuditOperation.CREATE,
                AuditEntityType.LIST_ELEMENT,
                element.getId().toString(),
                null,
                AuditEventDescriptions.forListElement(element),
                null
        );

        return ListElementResponse.of(element);
    }

    @Transactional
    @RequiresAuditComment(operation = AuditOperation.UPDATE, targetType = AuditEntityType.LIST_ELEMENT)
    public ListElementResponse updateElement(ResourceIdentifier parentId, ResourceIdentifier elementId, UpdateListElementRequest input) {
        ListElement element = this.repository.listElementRepo.getElement(parentId, elementId)
                .orElseThrow(() -> new ResourceNotFoundException(ComponentTypes.LIST_ELEMENT, elementId));
        ComponentReference.Reference<?> ref = element.getReference();

        Locale locale = LocaleContextHolder.getLocale();
        String oldName = this.overrideService.upsertSilent(ref.getTranslationKey(TranslationField.NAME), locale, input.name());
        this.overrideService.upsertSilent(ref.getTranslationKey(TranslationField.DESCRIPTION), locale, input.description());

        element.setElementIndex(input.index());
        element.setActiveTo(input.activeTo());
        element.setAliases(new HashSet<>(input.aliases()));

        this.repository.listElementRepo.saveAndFlush(element);

        this.auditService.addEvent(
                AuditOperation.UPDATE,
                AuditEntityType.LIST_ELEMENT,
                elementId.toString(),
                AuditEventDescriptions.singleChange("name", oldName, input.name()),
                AuditEventDescriptions.forListElement(element),
                null
        );

        return ListElementResponse.of(element);
    }

    @Transactional
    @RequiresAuditComment(operation = AuditOperation.DELETE, targetType = AuditEntityType.LIST_ELEMENT)
    public void deleteElement(ResourceIdentifier parentId, ResourceIdentifier elementId) {
        ListElement element = this.repository.listElementRepo.getElement(parentId, elementId)
                .orElseThrow(() -> new ResourceNotFoundException(ComponentTypes.LIST_ELEMENT, elementId));

        this.repository.listElementRepo.delete(element);

        this.auditService.addEvent(
                AuditOperation.DELETE,
                AuditEntityType.LIST_ELEMENT,
                elementId.toString(),
                null,
                AuditEventDescriptions.forListElement(element),
                null
        );
    }
}
