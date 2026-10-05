package com.openrecordsmanager.location.type;

import com.openrecordsmanager.api.ResourceIdentifier;
import com.openrecordsmanager.api.audit.AuditEntityType;
import com.openrecordsmanager.api.audit.AuditOperation;
import com.openrecordsmanager.api.location.LocationKind;
import com.openrecordsmanager.api.types.ComponentTypes;
import com.openrecordsmanager.audit.AuditService;
import com.openrecordsmanager.audit.RequiresAuditComment;
import com.openrecordsmanager.database.DataRepository;
import com.openrecordsmanager.location.type.dto.LocationTypeResponse;
import com.openrecordsmanager.location.type.dto.NewLocationTypeRequest;
import com.openrecordsmanager.location.type.dto.UpdateLocationTypeRequest;
import com.openrecordsmanager.plugin.exception.BuiltinResourceImmutableException;
import com.openrecordsmanager.property.ObjectProperty;
import com.openrecordsmanager.property.dto.TypePropertyAssignment;
import com.openrecordsmanager.rest.exception.ResourceAlreadyExistsException;
import com.openrecordsmanager.rest.exception.ResourceNotFoundException;
import org.jspecify.annotations.Nullable;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.HashSet;
import java.util.List;
import java.util.Set;

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

    @Transactional
    @RequiresAuditComment(operation = AuditOperation.CREATE, targetType = AuditEntityType.LOCATION_TYPE)
    public LocationTypeResponse create(NewLocationTypeRequest input) {
        if (input.id().isBuiltin()) {
            throw new BuiltinResourceImmutableException();
        }
        if (input.kind() == LocationKind.ANY) {
            throw new IllegalArgumentException("Location type kind must be USER or GROUP");
        }
        if (this.repository.locationTypeRepo.existsById(input.id())) {
            throw new ResourceAlreadyExistsException(ComponentTypes.LOCATION_TYPE, input.id());
        }

        LocationType type = new LocationType(input.id(), input.kind(), resolveProperties(input.properties()));
        this.repository.locationTypeRepo.saveAndFlush(type);

        this.auditService.addEvent(AuditOperation.CREATE, AuditEntityType.LOCATION_TYPE, type.getId());
        return LocationTypeResponse.of(type);
    }

    @Transactional
    @RequiresAuditComment(operation = AuditOperation.UPDATE, targetType = AuditEntityType.LOCATION_TYPE)
    public LocationTypeResponse update(ResourceIdentifier id, UpdateLocationTypeRequest input) {
        LocationType type = this.repository.locationTypeRepo.findById(id)
                .orElseThrow(() -> new ResourceNotFoundException(ComponentTypes.LOCATION_TYPE, id));

        if (id.isBuiltin()) {
            throw new BuiltinResourceImmutableException();
        }
        if (input.kind() == LocationKind.ANY) {
            throw new IllegalArgumentException("Location type kind must be USER or GROUP");
        }

        type.setKind(input.kind());
        type.setProperties(mergeProperties(type.getProperties(), input.properties()));

        this.repository.locationTypeRepo.saveAndFlush(type);
        this.auditService.addEvent(AuditOperation.UPDATE, AuditEntityType.LOCATION_TYPE, id);
        return LocationTypeResponse.of(type);
    }

    private Set<LocationTypeProperty<?>> mergeProperties(
            Set<LocationTypeProperty<?>> existing,
            List<TypePropertyAssignment> assignments
    ) {
        Set<LocationTypeProperty<?>> properties = resolveProperties(assignments);
        existing.stream()
                .filter(property -> property.getProperty().isUserHidden())
                .forEach(properties::add);
        return properties;
    }

    @SuppressWarnings({"rawtypes", "unchecked"})
    private Set<LocationTypeProperty<?>> resolveProperties(List<TypePropertyAssignment> assignments) {
        Set<LocationTypeProperty<?>> properties = new HashSet<>();
        for (TypePropertyAssignment assignment : assignments) {
            ObjectProperty property = this.repository.objectPropertyRepo.findById(assignment.property())
                    .filter(p -> !p.isUserHidden())
                    .orElseThrow(() -> new ResourceNotFoundException(ComponentTypes.OBJECT_PROPERTY, assignment.property()));
            properties.add(new LocationTypeProperty(property, assignment.defaultValue()));
        }
        return properties;
    }
}
