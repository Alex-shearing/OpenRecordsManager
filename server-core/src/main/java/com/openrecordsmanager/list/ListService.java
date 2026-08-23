package com.openrecordsmanager.list;

import com.openrecordsmanager.api.ResourceIdentifier;
import com.openrecordsmanager.api.types.ComponentTypes;
import com.openrecordsmanager.database.DataRepository;
import com.openrecordsmanager.list.dto.ListElementResponse;
import com.openrecordsmanager.list.dto.ListTypeResponse;
import com.openrecordsmanager.list.dto.SimpleListTypeResponse;
import com.openrecordsmanager.rest.errors.ResourceNotFoundException;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

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

    @Transactional(readOnly = true)
    public ListElementResponse getElement(ResourceIdentifier parent, ResourceIdentifier id) {
        ListElement type = this.repository.listElementRepo.getElement(parent, id)
                .orElseThrow(() -> new ResourceNotFoundException(ComponentTypes.LIST, id));

        return ListElementResponse.of(type);
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
}
