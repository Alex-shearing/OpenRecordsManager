package com.openrecordsmanager.record;

import com.openrecordsmanager.api.ResourceIdentifier;
import com.openrecordsmanager.api.audit.AuditEntityType;
import com.openrecordsmanager.api.audit.AuditOperation;
import com.openrecordsmanager.api.builtin.BuiltinConfigs;
import com.openrecordsmanager.api.record.RecordActionType;
import com.openrecordsmanager.api.types.ComponentTypes;
import com.openrecordsmanager.audit.AuditEventDescriptions;
import com.openrecordsmanager.audit.AuditPolicyService;
import com.openrecordsmanager.audit.AuditService;
import com.openrecordsmanager.audit.RequiresAuditComment;
import com.openrecordsmanager.config.ConfigService;
import com.openrecordsmanager.database.DataRepository;
import com.openrecordsmanager.filestore.store.FileStore;
import com.openrecordsmanager.plugin.ExpressionsService;
import com.openrecordsmanager.plugin.registry.ComponentCatalog;
import com.openrecordsmanager.property.ObjectProperty;
import com.openrecordsmanager.record.dto.NewRecordRequest;
import com.openrecordsmanager.record.dto.RecordResponse;
import com.openrecordsmanager.record.dto.RecordRevisionResponse;
import com.openrecordsmanager.recordtype.RecordType;
import com.openrecordsmanager.rest.dto.ActionResponse;
import com.openrecordsmanager.rest.errors.ResourceNotFoundException;
import com.openrecordsmanager.user.User;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.io.InputStream;
import java.text.MessageFormat;
import java.util.Map;
import java.util.Objects;
import java.util.Set;
import java.util.UUID;
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
    public RecordResponse get(User user, UUID id) {
        Record record = this.repository.recordRepo.findById(id)
                .filter(r -> r.securityFilter(this.expressions, user, r).canSeeMetadata())
                .orElseThrow(() -> new ResourceNotFoundException("record", id));

        this.auditService.addEvent(AuditOperation.READ, AuditEntityType.RECORD, record.getId());

        return RecordResponse.of(record);
    }

    @Transactional
    @RequiresAuditComment(operation = AuditOperation.CREATE, targetType = AuditEntityType.RECORD)
    public RecordResponse create(NewRecordRequest input) {
        RecordType type = this.repository.recordTypeRepo.findById(input.type())
                .orElseThrow(() -> new ResourceNotFoundException(ComponentTypes.RECORD_TYPE, input.type()));

        Record record = new Record("tba", type);
        input.properties().forEach((identifier, o) -> {
            ObjectProperty<?> property = this.repository.objectPropertyRepo.findById(identifier)
                    .orElseThrow(() -> new ResourceNotFoundException(ComponentTypes.OBJECT_PROPERTY, identifier));

            setProperty(record, property, o);
        });

        this.repository.recordRepo.saveAndFlush(record);

        this.auditService.addEvent(
                AuditOperation.CREATE,
                AuditEntityType.RECORD,
                record.getId().toString(),
                null,
                AuditEventDescriptions.forRecord(record),
                null
        );

        return RecordResponse.of(record);
    }

    private static <K> void setProperty(Record record, ObjectProperty<K> property, Object value) {
        record.setProperty(property, Objects.requireNonNull(property.getType().cast(value)));
    }

    @Transactional
    @RequiresAuditComment(operation = AuditOperation.CREATE, targetType = AuditEntityType.RECORD_REVISION)
    public RecordResponse createRevision(User user, UUID id, String version, String fileExtension, InputStream file) {
        Record record = this.repository.recordRepo.findById(id)
                .filter(r -> r.securityFilter(this.expressions, user, r).canSeeFiles())
                .orElseThrow(() -> new ResourceNotFoundException("record", id));

        if (!record.getType().supportsFile()) {
            throw new IllegalArgumentException(MessageFormat.format("Record type {0} does not support attaching a file", record.getType().id));
        }

        UUID defaultStoreId = this.config.getOptional(BuiltinConfigs.DEFAULT_FILE_STORE)
                .orElseThrow(() -> new IllegalStateException("There is no default file store set"));

        FileStore fileStore = this.repository.fileStoreRepo.findById(defaultStoreId)
                .orElseThrow(() -> new ResourceNotFoundException("file store", defaultStoreId));

        record.addRevision(version, fileStore.newFile(this.catalog, file, fileExtension));

        Record saved = this.repository.recordRepo.saveAndFlush(record);
        RecordRevision revision = saved.getCurrentRevision();

        this.auditService.addEvent(
                AuditOperation.CREATE,
                AuditEntityType.RECORD_REVISION,
                revision.id.toString(),
                AuditEventDescriptions.singleChange("version", null, version),
                AuditEventDescriptions.forRecordRevision(revision),
                null
        );

        return RecordResponse.of(saved);
    }

    @Transactional(readOnly = true)
    public RecordRevisionResponse getRevision(User user, UUID id, String version) {
        RecordRevision rev = this.repository.recordRepo.findByRecordId(id, version)
                .filter(r -> r.record.securityFilter(this.expressions, user, r.record).canSeeFiles())
                .orElseThrow(() -> new ResourceNotFoundException("record revision", id + "/" + version));

        this.auditService.addReadEvent(
                AuditEntityType.RECORD_REVISION,
                rev.id.toString(),
                AuditEventDescriptions.forRecordRevision(rev)
        );

        return RecordRevisionResponse.of(this.catalog, rev);
    }

    @Transactional(readOnly = true)
    public Set<ActionResponse> listActions(User user, UUID recordId) {
        Record record = this.repository.recordRepo.findById(recordId)
                .filter(r -> r.securityFilter(this.expressions, user, r).canSeeMetadata())
                .orElseThrow(() -> new ResourceNotFoundException("record", recordId));

        RecordActionContextImpl context = new RecordActionContextImpl(
                this.repository,
                this.catalog,
                this.config,
                this.auditService,
                user,
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
    public void executeAction(User user, UUID recordId, ResourceIdentifier actionId, Map<String, ?> inputs) {
        RecordActionType<?> action = this.catalog.getRegistry(ComponentTypes.RECORD_ACTION).get(actionId)
                .orElseThrow(() -> new ResourceNotFoundException(ComponentTypes.RECORD_ACTION, actionId));

        Record record = this.repository.recordRepo.findById(recordId)
                .filter(r -> r.securityFilter(this.expressions, user, r).canSeeMetadata())
                .orElseThrow(() -> new ResourceNotFoundException("record", recordId));

        RecordActionContextImpl context = new RecordActionContextImpl(
                this.repository,
                this.catalog,
                this.config,
                this.auditService,
                user,
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
