package com.openrecordsmanager.location.type;

import com.openrecordsmanager.api.ResourceIdentifier;
import com.openrecordsmanager.api.audit.AuditEntityType;
import com.openrecordsmanager.api.location.LocationKind;
import com.openrecordsmanager.api.types.ComponentTypes;
import com.openrecordsmanager.audit.AuditService;
import com.openrecordsmanager.database.DataRepository;
import com.openrecordsmanager.location.type.dto.LocationTypeResponse;
import com.openrecordsmanager.rest.exception.ResourceNotFoundException;
import org.jspecify.annotations.Nullable;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;

@Service
public class LocationTypeService {

    private final DataRepository repository;
    private final AuditService auditService;

    public LocationTypeService(DataRepository repository, AuditService auditService) {
        this.repository = repository;
        this.auditService = auditService;
    }

    @Transactional(readOnly = true)
    public List<LocationTypeResponse> getAll(@Nullable LocationKind kind) {
        List<LocationTypeResponse> types = this.repository.locationTypeRepo.findAll().stream()
                .filter(type -> kind == null || type.getKind() == kind)
                .map(LocationTypeResponse::of)
                .toList();
        this.auditService.recordCollectionRead(AuditEntityType.LOCATION_TYPE, types.size());
        return types;
    }

    @Transactional(readOnly = true)
    public LocationTypeResponse get(ResourceIdentifier id) {
        LocationType locationType = this.repository.locationTypeRepo.findById(id)
                .orElseThrow(() -> new ResourceNotFoundException(ComponentTypes.LOCATION_TYPE, id));

        this.auditService.addReadEvent(AuditEntityType.LOCATION_TYPE, id);
        return LocationTypeResponse.of(locationType);
    }
}
