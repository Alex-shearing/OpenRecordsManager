package com.openrecordsmanager.record.type;

import com.openrecordsmanager.api.ResourceIdentifier;
import com.openrecordsmanager.api.audit.AuditEntityType;
import com.openrecordsmanager.api.audit.AuditOperation;
import com.openrecordsmanager.api.types.ComponentTypes;
import com.openrecordsmanager.audit.AuditService;
import com.openrecordsmanager.audit.RequiresAuditComment;
import com.openrecordsmanager.database.DataRepository;
import com.openrecordsmanager.plugin.exception.BuiltinResourceImmutableException;
import com.openrecordsmanager.property.ObjectProperty;
import com.openrecordsmanager.property.dto.TypePropertyAssignment;
import com.openrecordsmanager.record.type.dto.NewRecordTypeRequest;
import com.openrecordsmanager.record.type.dto.RecordTypeResponse;
import com.openrecordsmanager.record.type.dto.UpdateRecordTypeRequest;
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
public class RecordTypeService {

    private final DataRepository repository;
    private final AuditService auditService;

    public RecordTypeService(DataRepository repository, AuditService auditService) {
        this.repository = repository;
        this.auditService = auditService;
    }

    @Transactional(readOnly = true)
    public List<RecordTypeResponse> getAll() {
        List<RecordTypeResponse> types = this.repository.recordTypeRepo.findAll().stream()
                .map(RecordTypeResponse::of)
                .toList();
        this.auditService.recordCollectionRead(AuditEntityType.RECORD_TYPE, types.size());
        return types;
    }

    @Transactional(readOnly = true)
    public RecordTypeResponse get(ResourceIdentifier id) {
        RecordType recordType = this.repository.recordTypeRepo.findById(id)
                .orElseThrow(() -> new ResourceNotFoundException(ComponentTypes.RECORD_TYPE, id));

        this.auditService.addReadEvent(AuditEntityType.RECORD_TYPE, id);
        return RecordTypeResponse.of(recordType);
    }

    @Transactional
    @RequiresAuditComment(operation = AuditOperation.CREATE, targetType = AuditEntityType.RECORD_TYPE)
    public RecordTypeResponse create(NewRecordTypeRequest input) {
        if (input.id().isBuiltin()) {
            throw new BuiltinResourceImmutableException();
        }
        if (this.repository.recordTypeRepo.existsById(input.id())) {
            throw new ResourceAlreadyExistsException(ComponentTypes.RECORD_TYPE, input.id());
        }

        RecordType type = new RecordType(
                input.id(),
                input.abstractType(),
                this.resolveBaseType(input.baseTypeId()),
                input.contentTypes(),
                input.securityFilter(),
                input.securityFilterUsage(),
                this.resolveProperties(input.properties())
        );
        this.repository.recordTypeRepo.saveAndFlush(type);

        this.auditService.addEvent(AuditOperation.CREATE, AuditEntityType.RECORD_TYPE, type.getId());
        return RecordTypeResponse.of(type);
    }

    @Transactional
    @RequiresAuditComment(operation = AuditOperation.UPDATE, targetType = AuditEntityType.RECORD_TYPE)
    public RecordTypeResponse update(ResourceIdentifier id, UpdateRecordTypeRequest input) {
        RecordType type = this.repository.recordTypeRepo.findById(id)
                .orElseThrow(() -> new ResourceNotFoundException(ComponentTypes.RECORD_TYPE, id));

        if (id.isBuiltin()) {
            throw new BuiltinResourceImmutableException();
        }

        type.setAbstractType(input.abstractType());
        type.setBaseType(this.resolveBaseType(input.baseTypeId()));
        type.setContentTypes(input.contentTypes());
        type.setSecurityFilter(input.securityFilter());
        type.setSecurityFilterUsage(input.securityFilterUsage());
        type.setProperties(this.mergeProperties(type.getDeclaredProperties(), input.properties()));

        this.repository.recordTypeRepo.saveAndFlush(type);
        this.auditService.addEvent(AuditOperation.UPDATE, AuditEntityType.RECORD_TYPE, id);
        return RecordTypeResponse.of(type);
    }

    private @Nullable ManagedObjectType resolveBaseType(@Nullable ResourceIdentifier baseTypeId) {
        if (baseTypeId == null) {
            return null;
        }
        return this.repository.recordTypeRepo.findById(baseTypeId)
                .orElseThrow(() -> new ResourceNotFoundException(ComponentTypes.RECORD_TYPE, baseTypeId));
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
