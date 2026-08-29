package com.openrecordsmanager.recordtype;

import com.openrecordsmanager.api.ResourceIdentifier;
import com.openrecordsmanager.api.audit.AuditEntityType;
import com.openrecordsmanager.api.types.ComponentTypes;
import com.openrecordsmanager.audit.AuditService;
import com.openrecordsmanager.database.DataRepository;
import com.openrecordsmanager.recordtype.dto.RecordTypeResponse;
import com.openrecordsmanager.rest.errors.ResourceNotFoundException;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;

@Service
public class RecordTypeService {

    private final DataRepository repository;
    private final AuditService auditService;

    public RecordTypeService(DataRepository repository, AuditService auditService) {
        this.repository = repository;
        this.auditService = auditService;
    }

    @Transactional(readOnly = true)
    public List<ResourceIdentifier> getAllIds() {
        List<ResourceIdentifier> ids = this.repository.recordTypeRepo.findAllIds();
        this.auditService.recordCollectionRead(AuditEntityType.RECORD_TYPE, ids.size());
        return ids;
    }

    @Transactional(readOnly = true)
    public RecordTypeResponse get(ResourceIdentifier id) {
        RecordType recordType = this.repository.recordTypeRepo.findById(id)
                .orElseThrow(() -> new ResourceNotFoundException(ComponentTypes.RECORD_TYPE, id));

        this.auditService.addReadEvent(AuditEntityType.RECORD_TYPE, id);
        return RecordTypeResponse.of(recordType);
    }
}
