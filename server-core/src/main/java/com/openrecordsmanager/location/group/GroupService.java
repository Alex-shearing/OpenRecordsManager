package com.openrecordsmanager.location.group;

import com.openrecordsmanager.api.audit.AuditEntityType;
import com.openrecordsmanager.api.audit.AuditOperation;
import com.openrecordsmanager.api.errors.ApiException;
import com.openrecordsmanager.api.location.LocationKind;
import com.openrecordsmanager.api.search.SearchFieldTarget;
import com.openrecordsmanager.api.types.ComponentTypes;
import com.openrecordsmanager.audit.AuditPropertyChange;
import com.openrecordsmanager.audit.AuditService;
import com.openrecordsmanager.audit.RequiresAuditComment;
import com.openrecordsmanager.database.DataRepository;
import com.openrecordsmanager.location.group.dto.*;
import com.openrecordsmanager.location.type.LocationType;
import com.openrecordsmanager.location.user.User;
import com.openrecordsmanager.property.ObjectPropertyApplier;
import com.openrecordsmanager.rest.exception.ResourceNotFoundException;
import com.openrecordsmanager.search.ObjectSearchExecutor;
import com.openrecordsmanager.search.sql.BuiltinColumnResolver;
import com.openrecordsmanager.search.sql.ObjectSearchSchema;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.*;
import java.util.stream.Collectors;

@Service
public class GroupService {

    private final DataRepository repository;
    private final AuditService auditService;
    private final ObjectPropertyApplier propertyApplier;
    private final ObjectSearchExecutor searchExecutor;
    private final ObjectSearchSchema searchSchema;

    public GroupService(
            DataRepository repository,
            AuditService auditService,
            ObjectPropertyApplier propertyApplier,
            ObjectSearchExecutor searchExecutor,
            BuiltinColumnResolver columnResolver
    ) {
        this.repository = repository;
        this.auditService = auditService;
        this.propertyApplier = propertyApplier;
        this.searchExecutor = searchExecutor;
        this.searchSchema = ObjectSearchSchema.of(
                SearchFieldTarget.LOCATION,
                Group.class,
                Group.BUILTIN_PROPERTY_BINDINGS,
                columnResolver
        );
    }

    @Transactional(readOnly = true)
    public GroupSearchResponse search(User actor, GroupSearchRequest request) {
        int pageLimit = request.limitOrDefault();
        List<UUID> ids = this.searchExecutor.searchIds(
                this.searchSchema,
                actor,
                request.q(),
                request.filters(),
                request.matchOrDefault(),
                null,
                request.cursor(),
                pageLimit + 1
        );

        boolean hasMore = ids.size() > pageLimit;
        if (hasMore) {
            ids = ids.subList(0, pageLimit);
        }

        Map<UUID, Group> loaded = this.repository.groupRepo.findAllById(ids).stream()
                .collect(Collectors.toMap(Group::getId, g -> g));

        List<GroupResponse> items = new ArrayList<>(ids.size());
        for (UUID id : ids) {
            Group group = loaded.get(id);
            if (group != null) {
                items.add(GroupResponse.of(group));
            }
        }

        UUID nextCursor = hasMore && !items.isEmpty() ? items.getLast().id() : null;

        this.auditService.recordSearchRead(
                AuditEntityType.GROUP,
                AuditService.COLLECTION_TARGET_ID,
                this.searchExecutor.summarize(request.q(), request.filters()),
                items.size()
        );

        return new GroupSearchResponse(List.copyOf(items), nextCursor);
    }

    @Transactional(readOnly = true)
    public GroupResponse get(UUID id) {
        Group group = this.repository.groupRepo.findById(id)
                .orElseThrow(() -> new ResourceNotFoundException("group", id));

        this.auditService.addReadEvent(AuditEntityType.GROUP, id);
        return GroupResponse.of(group);
    }

    @Transactional
    @RequiresAuditComment(operation = AuditOperation.CREATE, targetType = AuditEntityType.GROUP)
    public GroupResponse create(NewGroupRequest input) {
        LocationType type = this.repository.locationTypeRepo.findById(input.type())
                .orElseThrow(() -> new ResourceNotFoundException(ComponentTypes.LOCATION_TYPE, input.type()));
        if (type.getKind() != LocationKind.GROUP) {
            throw ApiException.validationFailed("type", "invalid_location_type_kind", type.getKind().key());
        }

        List<AuditPropertyChange> changes = new ArrayList<>();
        changes.add(AuditPropertyChange.newProperty("type", input.type()));
        changes.add(AuditPropertyChange.newProperty("name", input.name()));

        Group group = new Group(input.name(), type);
        this.propertyApplier.applyOnCreate(group, input.properties(), true, changes);

        this.repository.groupRepo.saveAndFlush(group);

        this.auditService.addEvent(
                AuditOperation.CREATE,
                AuditEntityType.GROUP,
                group.getId().toString(),
                changes,
                null,
                null
        );

        return GroupResponse.of(group);
    }

    @Transactional
    @RequiresAuditComment(operation = AuditOperation.UPDATE, targetType = AuditEntityType.GROUP)
    public GroupResponse update(UUID id, UpdateGroupRequest input) {
        Group group = this.repository.groupRepo.findById(id)
                .orElseThrow(() -> new ResourceNotFoundException("group", id));

        List<AuditPropertyChange> changes = new ArrayList<>();

        if (input.name() != null && !input.name().equals(group.getName())) {
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
                AuditEntityType.GROUP,
                id.toString(),
                changes.isEmpty() ? null : changes,
                null,
                null
        );

        return GroupResponse.of(group);
    }
}
