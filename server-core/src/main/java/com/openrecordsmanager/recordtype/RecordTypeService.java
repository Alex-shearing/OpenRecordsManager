package com.openrecordsmanager.recordtype;

import com.openrecordsmanager.api.ResourceIdentifier;
import com.openrecordsmanager.api.types.ComponentTypes;
import com.openrecordsmanager.database.DataRepository;
import com.openrecordsmanager.recordtype.dto.RecordTypeResponse;
import com.openrecordsmanager.rest.errors.ResourceNotFoundException;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;

@Service
public class RecordTypeService {

    private final DataRepository repository;

    public RecordTypeService(DataRepository repository) {
        this.repository = repository;
    }

    @Transactional(readOnly = true)
    public List<ResourceIdentifier> getAllIds() {
        return this.repository.recordTypeRepo.findAllIds();
    }

    @Transactional(readOnly = true)
    public RecordTypeResponse get(ResourceIdentifier id) {
        return this.repository.recordTypeRepo.findById(id)
                .map(RecordTypeResponse::of)
                .orElseThrow(() -> new ResourceNotFoundException(ComponentTypes.RECORD_TYPE, id));
    }
}
