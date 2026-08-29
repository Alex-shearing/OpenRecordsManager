package com.openrecordsmanager.list;

import com.openrecordsmanager.api.ResourceIdentifier;
import com.openrecordsmanager.api.audit.AuditEntityType;
import com.openrecordsmanager.api.audit.AuditOperation;
import com.openrecordsmanager.api.types.ComponentTypes;
import com.openrecordsmanager.audit.AuditEventDescriptions;
import com.openrecordsmanager.audit.AuditService;
import com.openrecordsmanager.audit.RequiresAuditComment;
import com.openrecordsmanager.database.DataRepository;
import com.openrecordsmanager.list.dto.*;
import com.openrecordsmanager.rest.errors.ResourceInUseException;
import com.openrecordsmanager.rest.errors.ResourceNotFoundException;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.HashSet;
import java.util.Set;
import java.util.stream.Collectors;

@Service
public class ListService {

    private final DataRepository repository;
    private final AuditService auditService;

    public ListService(DataRepository repository, AuditService auditService) {
        this.repository = repository;
        this.auditService = auditService;
    }

    @Transactional(readOnly = true)
    public Set<SimpleListTypeResponse> getAll() {
        Set<SimpleListTypeResponse> results = this.repository.listTypeRepo.findAll().stream()
                .map(SimpleListTypeResponse::of)
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
            throw new ResourceInUseException("list type already exists: " + input.id());
        }

        ListType listType = new ListType(input.id(), input.name());
        this.repository.listTypeRepo.saveAndFlush(listType);

        this.auditService.addEvent(AuditOperation.CREATE, AuditEntityType.LIST, listType.getId());

        return ListTypeResponse.of(listType);
    }

    @Transactional
    @RequiresAuditComment(operation = AuditOperation.UPDATE, targetType = AuditEntityType.LIST)
    public ListTypeResponse update(ResourceIdentifier id, UpdateListTypeRequest input) {
        ListType listType = this.repository.listTypeRepo.findById(id)
                .orElseThrow(() -> new ResourceNotFoundException(ComponentTypes.LIST, id));

        String oldName = listType.getName();
        listType.setName(input.name());
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
            throw new ResourceInUseException("list type has elements and cannot be deleted");
        }

        if (this.repository.listTypeRepo.isUsedByObjectProperties(listType)) {
            throw new ResourceInUseException("list type is in use by one or more object properties");
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

        Set<ListElementResponse> results = this.repository.listElementRepo.searchNameAndAlias(type, search)
                .stream()
                .map(ListElementResponse::of)
                .collect(Collectors.toSet());

        this.auditService.recordSearchRead(AuditEntityType.LIST, parent.toString(), search, results.size());
        return results;
    }

    @Transactional
    @RequiresAuditComment(operation = AuditOperation.CREATE, targetType = AuditEntityType.LIST_ELEMENT)
    public ListElementResponse createElement(ResourceIdentifier parentId, NewListElementRequest input) {
        ListType parent = this.repository.listTypeRepo.findById(parentId)
                .orElseThrow(() -> new ResourceNotFoundException(ComponentTypes.LIST, parentId));

        if (this.repository.listElementRepo.existsById(input.id())) {
            throw new ResourceInUseException("list element already exists: " + input.id());
        }

        ListElement element = new ListElement(
                input.id(),
                parent,
                input.name(),
                input.description(),
                input.index(),
                input.activeTo(),
                new HashSet<>(input.aliases())
        );
        this.repository.listElementRepo.saveAndFlush(element);

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

        String oldName = element.getName();
        element.setName(input.name());
        element.setDescription(input.description());
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
