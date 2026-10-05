package com.openrecordsmanager.location.group;

import com.openrecordsmanager.api.audit.AuditEntityType;
import com.openrecordsmanager.api.audit.AuditOperation;
import com.openrecordsmanager.api.errors.ApiException;
import com.openrecordsmanager.api.search.SearchFieldTarget;
import com.openrecordsmanager.audit.AuditPropertyChange;
import com.openrecordsmanager.audit.AuditService;
import com.openrecordsmanager.database.DataRepository;
import com.openrecordsmanager.location.LocationSearchSupport;
import com.openrecordsmanager.location.dto.LocationResponse;
import com.openrecordsmanager.location.dto.LocationSearchResponse;
import com.openrecordsmanager.location.dto.NewLocationRequest;
import com.openrecordsmanager.location.dto.UpdateLocationRequest;
import com.openrecordsmanager.location.type.LocationType;
import com.openrecordsmanager.location.user.User;
import com.openrecordsmanager.property.ObjectPropertyApplier;
import com.openrecordsmanager.rest.exception.ResourceAlreadyExistsException;
import com.openrecordsmanager.search.dto.ObjectSearchRequest;
import com.openrecordsmanager.search.sql.BuiltinColumnResolver;
import com.openrecordsmanager.search.sql.ObjectSearchSchema;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.ArrayList;
import java.util.List;
import java.util.function.Function;
import java.util.stream.Collectors;

@Service
public class GroupService {

    private final DataRepository repository;
    private final AuditService auditService;
    private final ObjectPropertyApplier propertyApplier;
    private final LocationSearchSupport searchSupport;
    private final ObjectSearchSchema searchSchema;

    public GroupService(
            DataRepository repository,
            AuditService auditService,
            ObjectPropertyApplier propertyApplier,
            LocationSearchSupport searchSupport,
            BuiltinColumnResolver columnResolver
    ) {
        this.repository = repository;
        this.auditService = auditService;
        this.propertyApplier = propertyApplier;
        this.searchSupport = searchSupport;
        this.searchSchema = ObjectSearchSchema.of(
                SearchFieldTarget.LOCATION,
                Group.class,
                Group.BUILTIN_PROPERTY_BINDINGS,
                columnResolver
        );
    }

    @Transactional(readOnly = true)
    public LocationSearchResponse search(User actor, ObjectSearchRequest request) {
        return this.searchSupport.search(
                this.searchSchema,
                actor,
                request,
                ids -> this.repository.groupRepo.findAllById(ids).stream()
                        .collect(Collectors.toMap(Group::getId, Function.identity()))
        );
    }

    @Transactional
    public LocationResponse create(LocationType type, NewLocationRequest input) {
        String name = input.name();
        if (name == null || name.isBlank()) {
            throw ApiException.validationFailed("name", "required");
        }
        if (this.repository.locationRepo.findByName(name).isPresent()) {
            throw new ResourceAlreadyExistsException("location", name);
        }

        List<AuditPropertyChange> changes = new ArrayList<>();
        changes.add(AuditPropertyChange.newProperty("type", input.type()));
        changes.add(AuditPropertyChange.newProperty("name", name));

        Group group = new Group(name, type);
        this.propertyApplier.applyOnCreate(group, input.properties(), true, changes);

        this.repository.groupRepo.saveAndFlush(group);

        this.auditService.addEvent(
                AuditOperation.CREATE,
                AuditEntityType.LOCATION,
                group.getId().toString(),
                changes,
                null,
                null
        );

        return LocationResponse.of(group);
    }

    @Transactional
    public LocationResponse update(Group group, UpdateLocationRequest input) {
        List<AuditPropertyChange> changes = new ArrayList<>();

        if (input.name() != null && !input.name().equals(group.getName())) {
            if (this.repository.locationRepo.findByName(input.name()).isPresent()) {
                throw new ResourceAlreadyExistsException("location", input.name());
            }
            String oldName = group.getName();
            group.setName(input.name());
            changes.add(AuditPropertyChange.of("name", oldName, input.name()));
        }

        if (input.properties() != null) {
            this.propertyApplier.applyOnUpdate(group, input.properties(), true, changes);
        }

        this.repository.groupRepo.saveAndFlush(group);

        this.auditService.addEvent(
                AuditOperation.UPDATE,
                AuditEntityType.LOCATION,
                group.getId().toString(),
                changes.isEmpty() ? null : changes,
                null,
                null
        );

        return LocationResponse.of(group);
    }
}
