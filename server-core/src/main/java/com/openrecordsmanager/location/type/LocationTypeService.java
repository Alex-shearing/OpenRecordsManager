package com.openrecordsmanager.location.type;

import com.openrecordsmanager.api.ResourceIdentifier;
import com.openrecordsmanager.api.audit.AuditEntityType;
import com.openrecordsmanager.api.audit.AuditOperation;
import com.openrecordsmanager.api.property.ManagedObjectKind;
import com.openrecordsmanager.api.template.location.LocationTypeTemplate;
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
import com.openrecordsmanager.template.ManagedObjectType;
import com.openrecordsmanager.template.ManagedObjectTypeProperty;
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
    public List<LocationTypeResponse> getAll(@Nullable ManagedObjectKind kind) {
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
        LocationTypeTemplate.validateKind(input.kind(), input.abstractType());
        if (this.repository.locationTypeRepo.existsById(input.id())) {
            throw new ResourceAlreadyExistsException(ComponentTypes.LOCATION_TYPE, input.id());
        }

        LocationType type = new LocationType(
                input.id(),
                input.kind(),
                input.abstractType(),
                this.resolveBaseType(input.baseTypeId()),
                this.resolveProperties(input.properties())
        );
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
        LocationTypeTemplate.validateKind(input.kind(), input.abstractType());

        // Clear concrete kind before clearing abstract; set null kind only after abstract is set.
        if (input.kind() != null) {
            type.setKind(input.kind());
        }
        type.setAbstractType(input.abstractType());
        if (input.kind() == null) {
            type.setKind(null);
        }
        type.setBaseType(this.resolveBaseType(input.baseTypeId()));
        type.setProperties(this.mergeProperties(type.getDeclaredProperties(), input.properties()));

        this.repository.locationTypeRepo.saveAndFlush(type);
        this.auditService.addEvent(AuditOperation.UPDATE, AuditEntityType.LOCATION_TYPE, id);
        return LocationTypeResponse.of(type);
    }

    private @Nullable ManagedObjectType resolveBaseType(@Nullable ResourceIdentifier baseTypeId) {
        if (baseTypeId == null) {
            return null;
        }
        return this.repository.locationTypeRepo.findById(baseTypeId)
                .orElseThrow(() -> new ResourceNotFoundException(ComponentTypes.LOCATION_TYPE, baseTypeId));
    }

    private Set<ManagedObjectTypeProperty<?>> mergeProperties(
            Set<ManagedObjectTypeProperty<?>> existing,
            List<TypePropertyAssignment> assignments
    ) {
        Set<ManagedObjectTypeProperty<?>> properties = this.resolveProperties(assignments);
        existing.stream()
                .filter(property -> property.getProperty().isSystemProperty())
                .forEach(properties::add);
        return properties;
    }

    @SuppressWarnings({"rawtypes", "unchecked"})
    private Set<ManagedObjectTypeProperty<?>> resolveProperties(List<TypePropertyAssignment> assignments) {
        Set<ManagedObjectTypeProperty<?>> properties = new HashSet<>();
        for (TypePropertyAssignment assignment : assignments) {
            ObjectProperty property = this.repository.objectPropertyRepo.findById(assignment.property())
                    .filter(p -> !p.isSystemProperty())
                    .orElseThrow(() -> new ResourceNotFoundException(ComponentTypes.OBJECT_PROPERTY, assignment.property()));
            properties.add(new ManagedObjectTypeProperty(property, assignment.defaultValue()));
        }
        return properties;
    }
}
