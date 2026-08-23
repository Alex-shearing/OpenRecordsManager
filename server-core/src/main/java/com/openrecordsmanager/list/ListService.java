package com.openrecordsmanager.list;

import com.openrecordsmanager.api.ResourceIdentifier;
import com.openrecordsmanager.api.types.ComponentTypes;
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

    public ListService(DataRepository repository) {
        this.repository = repository;
    }

    @Transactional(readOnly = true)
    public Set<SimpleListTypeResponse> getAll() {
        return this.repository.listTypeRepo.findAll().stream()
                .map(SimpleListTypeResponse::of)
                .collect(Collectors.toSet());
    }

    @Transactional(readOnly = true)
    public ListTypeResponse get(ResourceIdentifier id) {
        return this.repository.listTypeRepo.findById(id)
                .map(ListTypeResponse::of)
                .orElseThrow(() -> new ResourceNotFoundException(ComponentTypes.LIST, id));
    }

    @Transactional
    public ListTypeResponse create(NewListTypeRequest input) {
        if (this.repository.listTypeRepo.existsById(input.id())) {
            throw new ResourceInUseException("list type already exists: " + input.id());
        }

        ListType listType = new ListType(input.id(), input.name());
        this.repository.listTypeRepo.saveAndFlush(listType);

        return ListTypeResponse.of(listType);
    }

    @Transactional
    public ListTypeResponse update(ResourceIdentifier id, UpdateListTypeRequest input) {
        ListType listType = this.repository.listTypeRepo.findById(id)
                .orElseThrow(() -> new ResourceNotFoundException(ComponentTypes.LIST, id));

        listType.setName(input.name());
        this.repository.listTypeRepo.saveAndFlush(listType);

        return ListTypeResponse.of(listType);
    }

    @Transactional
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
    }

    @Transactional(readOnly = true)
    public ListElementResponse getElement(ResourceIdentifier parent, ResourceIdentifier id) {
        ListElement element = this.repository.listElementRepo.getElement(parent, id)
                .orElseThrow(() -> new ResourceNotFoundException(ComponentTypes.LIST_ELEMENT, id));

        return ListElementResponse.of(element);
    }

    @Transactional(readOnly = true)
    public Set<ListElementResponse> searchElement(ResourceIdentifier parent, String search) {
        ListType type = this.repository.listTypeRepo.findById(parent)
                .orElseThrow(() -> new ResourceNotFoundException(ComponentTypes.LIST, parent));

        return this.repository.listElementRepo.searchNameAndAlias(type, search)
                .stream()
                .map(ListElementResponse::of)
                .collect(Collectors.toSet());
    }

    @Transactional
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

        return ListElementResponse.of(element);
    }

    @Transactional
    public ListElementResponse updateElement(ResourceIdentifier parentId, ResourceIdentifier elementId, UpdateListElementRequest input) {
        ListElement element = this.repository.listElementRepo.getElement(parentId, elementId)
                .orElseThrow(() -> new ResourceNotFoundException(ComponentTypes.LIST_ELEMENT, elementId));

        element.setName(input.name());
        element.setDescription(input.description());
        element.setElementIndex(input.index());
        element.setActiveTo(input.activeTo());
        element.setAliases(new HashSet<>(input.aliases()));

        this.repository.listElementRepo.saveAndFlush(element);

        return ListElementResponse.of(element);
    }

    @Transactional
    public void deleteElement(ResourceIdentifier parentId, ResourceIdentifier elementId) {
        ListElement element = this.repository.listElementRepo.getElement(parentId, elementId)
                .orElseThrow(() -> new ResourceNotFoundException(ComponentTypes.LIST_ELEMENT, elementId));

        this.repository.listElementRepo.delete(element);
    }
}
