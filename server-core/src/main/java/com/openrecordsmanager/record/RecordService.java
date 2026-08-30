package com.openrecordsmanager.record;

import com.openrecordsmanager.api.ResourceIdentifier;
import com.openrecordsmanager.api.audit.AuditEntityType;
import com.openrecordsmanager.api.audit.AuditOperation;
import com.openrecordsmanager.api.builtin.BuiltinConfigs;
import com.openrecordsmanager.api.record.RecordActionType;
import com.openrecordsmanager.api.template.recordtype.SecurityFilterUsage;
import com.openrecordsmanager.api.types.ComponentTypes;
import com.openrecordsmanager.audit.*;
import com.openrecordsmanager.config.ConfigService;
import com.openrecordsmanager.database.DataRepository;
import com.openrecordsmanager.filestore.store.FileStore;
import com.openrecordsmanager.plugin.ExpressionsService;
import com.openrecordsmanager.plugin.registry.ComponentCatalog;
import com.openrecordsmanager.property.ObjectProperty;
import com.openrecordsmanager.record.dto.NewRecordRequest;
import com.openrecordsmanager.record.dto.RecordResponse;
import com.openrecordsmanager.record.dto.RecordRevisionResponse;
import com.openrecordsmanager.record.dto.UpdateRecordRequest;
import com.openrecordsmanager.recordtype.RecordType;
import com.openrecordsmanager.rest.dto.ActionResponse;
import com.openrecordsmanager.rest.errors.ResourceNotFoundException;
import com.openrecordsmanager.user.User;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.io.InputStream;
import java.text.MessageFormat;
import java.util.*;
import java.util.stream.Collectors;

@Service
public class RecordService {

    private final DataRepository repository;
    private final ConfigService config;
    private final ComponentCatalog catalog;
    private final ExpressionsService expressions;
    private final AuditService auditService;
    private final AuditPolicyService auditPolicyService;

    public RecordService(
            DataRepository repository,
            ConfigService config,
            ComponentCatalog catalog,
            ExpressionsService expressions,
            AuditService auditService,
            AuditPolicyService auditPolicyService
    ) {
        this.repository = repository;
        this.config = config;
        this.catalog = catalog;
        this.expressions = expressions;
        this.auditService = auditService;
        this.auditPolicyService = auditPolicyService;
    }

    @Transactional(readOnly = true)
    public RecordResponse get(User actor, UUID id) {
        Record record = this.repository.recordRepo.findById(id)
                .filter(r -> r.securityFilter(this.expressions, actor).canSeeMetadata())
                .orElseThrow(() -> new ResourceNotFoundException("record", id));

        this.auditService.addReadEvent(AuditEntityType.RECORD, id);

        return RecordResponse.of(record);
    }

    @Transactional
    @RequiresAuditComment(operation = AuditOperation.CREATE, targetType = AuditEntityType.RECORD)
    public RecordResponse create(NewRecordRequest input) {
        RecordType type = this.repository.recordTypeRepo.findById(input.type())
                .orElseThrow(() -> new ResourceNotFoundException(ComponentTypes.RECORD_TYPE, input.type()));

        String recordTitle = "tba";

        List<AuditPropertyChange> changes = new ArrayList<>();
        changes.add(AuditPropertyChange.newProperty("type", input.type()));

        Record record = new Record(recordTitle, type);
        input.properties().forEach((identifier, value) -> {
            ObjectProperty<?> property = this.repository.objectPropertyRepo.findById(identifier)
                    .orElseThrow(() -> new ResourceNotFoundException(ComponentTypes.OBJECT_PROPERTY, identifier));

            Object newValue = record.setPropertyUntyped(property, value);
            changes.add(AuditPropertyChange.newProperty(identifier.toString(), newValue));
        });

        record.touchDateModified();
        this.repository.recordRepo.saveAndFlush(record);

        this.auditService.addEvent(
                AuditOperation.CREATE,
                AuditEntityType.RECORD,
                record.getId().toString(),
                changes,
                null,
                null
        );

        return RecordResponse.of(record);
    }

    @Transactional
    @RequiresAuditComment(operation = AuditOperation.UPDATE, targetType = AuditEntityType.RECORD)
    public RecordResponse update(User actor, UUID id, UpdateRecordRequest input) {
        Record record = this.repository.recordRepo.findById(id)
                .filter(r -> r.securityFilter(this.expressions, actor).canSeeMetadata())
                .orElseThrow(() -> new ResourceNotFoundException("record", id));

        List<AuditPropertyChange> changes = new ArrayList<>();

        if (input.type() != null && !input.type().equals(record.getType().id)) {
            RecordType newType = this.repository.recordTypeRepo.findById(input.type())
                    .orElseThrow(() -> new ResourceNotFoundException(ComponentTypes.RECORD_TYPE, input.type()));

            ResourceIdentifier oldType = record.getType().id;
            record.setType(newType);
            changes.add(new AuditPropertyChange("type", oldType, input.type()));
        }

        if (input.properties() != null) {
            input.properties().forEach((identifier, value) -> {
                ObjectProperty<?> property = this.repository.objectPropertyRepo.findById(identifier)
                        .orElseThrow(() -> new ResourceNotFoundException(ComponentTypes.OBJECT_PROPERTY, identifier));

                Object oldValue = record.getProperty(property);
                Object newValue = record.setPropertyUntyped(property, value);
                if (oldValue != newValue) {
                    changes.add(new AuditPropertyChange(identifier.toString(), oldValue, newValue));
                }
            });
        }

        record.touchDateModified();
        this.repository.recordRepo.saveAndFlush(record);

        this.auditService.addEvent(
                AuditOperation.UPDATE,
                AuditEntityType.RECORD,
                id.toString(),
                changes.isEmpty() ? null : changes,
                null,
                null
        );

        return RecordResponse.of(record);
    }

    @Transactional
    @RequiresAuditComment(operation = AuditOperation.CREATE, targetType = AuditEntityType.RECORD_REVISION)
    public RecordResponse createRevision(User actor, UUID id, String version, String fileExtension, InputStream file) {
        Record record = this.repository.recordRepo.findById(id)
                .orElseThrow(() -> new ResourceNotFoundException("record", id));

        // Security filtering on the record
        SecurityFilterUsage filter = record.securityFilter(this.expressions, actor);
        if (!filter.canSeeMetadata()) {
            throw new ResourceNotFoundException("record", id);
        }
        if (!filter.canSeeFiles()) {
            throw new IllegalArgumentException("you don't have the right to upload new revisions of this record");
        }

        if (!record.getType().supportsFile()) {
            throw new IllegalArgumentException(MessageFormat.format("Record type {0} does not support attaching a file", record.getType().id));
        }

        UUID defaultStoreId = this.config.getOptional(BuiltinConfigs.DEFAULT_FILE_STORE)
                .orElseThrow(() -> new IllegalStateException("There is no default file store set"));

        FileStore fileStore = this.repository.fileStoreRepo.findById(defaultStoreId)
                .orElseThrow(() -> new ResourceNotFoundException("file store", defaultStoreId));

        List<String> oldRevisions = record.getRevisionList();
        RecordRevision rev = record.addRevision(version, fileStore.newFile(this.catalog, file, fileExtension));
        this.repository.recordRepo.saveAndFlush(record);

        this.auditService.addEvent(
                AuditOperation.CREATE,
                AuditEntityType.RECORD_REVISION,
                rev.getId().toString(),
                List.of(
                        AuditPropertyChange.newProperty("version", rev.getVersion()),
                        AuditPropertyChange.newProperty("createdDate", rev.getCreatedDate())
                ),
                AuditEventDescriptions.forRecordRevision(rev),
                null
        );
        this.auditService.addEvent(
                AuditOperation.UPDATE,
                AuditEntityType.RECORD,
                record.getId().toString(),
                AuditEventDescriptions.singleChange("revisions", oldRevisions, record.getRevisionList()),
                null,
                null
        );

        return RecordResponse.of(record);
    }

    @Transactional(readOnly = true)
    public RecordRevisionResponse getRevision(User actor, UUID id, String version) {
        RecordRevision rev = this.repository.recordRepo.findRevisionById(id, version)
                .filter(r -> r.getRecord().securityFilter(this.expressions, actor).canSeeFiles())
                .orElseThrow(() -> new ResourceNotFoundException("record revision", id + "/" + version));

        this.auditService.addReadEvent(
                AuditEntityType.RECORD_REVISION,
                rev.getId().toString(),
                AuditEventDescriptions.forRecordRevision(rev)
        );

        return RecordRevisionResponse.of(this.catalog, rev);
    }

    @Transactional(readOnly = true)
    public Set<ActionResponse> listActions(User actor, UUID recordId) {
        Record record = this.repository.recordRepo.findById(recordId)
                .filter(r -> r.securityFilter(this.expressions, actor).canSeeMetadata())
                .orElseThrow(() -> new ResourceNotFoundException("record", recordId));

        RecordActionContextImpl context = new RecordActionContextImpl(
                this.repository,
                this.catalog,
                this.config,
                this.auditService,
                actor,
                record
        );

        Set<ActionResponse> actions = this.catalog.getRegistry(ComponentTypes.RECORD_ACTION).stream()
                .filter(action -> action.isAvailable(context))
                .map(action -> ActionResponse.ofRecord(this.catalog, action, this.auditPolicyService))
                .collect(Collectors.toSet());

        this.auditService.addReadEvent(AuditEntityType.RECORD, recordId);
        return actions;
    }

    @Transactional
    public void executeAction(User actor, UUID recordId, ResourceIdentifier actionId, Map<String, ?> inputs) {
        RecordActionType<?> action = this.catalog.getRegistry(ComponentTypes.RECORD_ACTION).get(actionId)
                .orElseThrow(() -> new ResourceNotFoundException(ComponentTypes.RECORD_ACTION, actionId));

        Record record = this.repository.recordRepo.findById(recordId)
                .filter(r -> r.securityFilter(this.expressions, actor).canSeeMetadata())
                .orElseThrow(() -> new ResourceNotFoundException("record", recordId));

        RecordActionContextImpl context = new RecordActionContextImpl(
                this.repository,
                this.catalog,
                this.config,
                this.auditService,
                actor,
                record
        );

        if (!action.isAvailable(context)) {
            throw new IllegalArgumentException("Action " + actionId + " is not available for record " + recordId);
        }

        this.auditPolicyService.validateCommentRequired(AuditEntityType.RECORD, AuditOperation.ACTION);

        action.executeUntyped(context, inputs);

        this.auditService.addActionRanEvent(
                actionId,
                AuditEntityType.RECORD,
                recordId,
                Map.of("inputs", inputs.keySet())
        );
    }
}
