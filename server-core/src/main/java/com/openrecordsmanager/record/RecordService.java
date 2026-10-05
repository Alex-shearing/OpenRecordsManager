package com.openrecordsmanager.record;

import com.openrecordsmanager.api.ResourceIdentifier;
import com.openrecordsmanager.api.audit.AuditEntityType;
import com.openrecordsmanager.api.audit.AuditOperation;
import com.openrecordsmanager.api.builtin.BuiltinConfigs;
import com.openrecordsmanager.api.record.RecordActionType;
import com.openrecordsmanager.api.search.SearchFieldTarget;
import com.openrecordsmanager.api.template.recordtype.SecurityFilterUsage;
import com.openrecordsmanager.api.types.ComponentTypes;
import com.openrecordsmanager.audit.*;
import com.openrecordsmanager.config.ConfigService;
import com.openrecordsmanager.database.DataRepository;
import com.openrecordsmanager.filestore.store.FileStore;
import com.openrecordsmanager.location.user.User;
import com.openrecordsmanager.plugin.ExpressionsService;
import com.openrecordsmanager.plugin.registry.ComponentCatalog;
import com.openrecordsmanager.property.ObjectPropertyApplier;
import com.openrecordsmanager.record.dto.*;
import com.openrecordsmanager.record.exception.DefaultFileStoreNotSetException;
import com.openrecordsmanager.record.exception.RecordTypeNoFileSupportException;
import com.openrecordsmanager.record.type.RecordType;
import com.openrecordsmanager.rest.dto.ActionResponse;
import com.openrecordsmanager.rest.exception.ActionNotAvailableException;
import com.openrecordsmanager.rest.exception.ForbiddenException;
import com.openrecordsmanager.rest.exception.ResourceNotFoundException;
import com.openrecordsmanager.schema.JsonSchemaValidator;
import com.openrecordsmanager.search.ObjectSearchExecutor;
import com.openrecordsmanager.search.dto.ObjectSearchRequest;
import com.openrecordsmanager.search.sql.BuiltinColumnResolver;
import com.openrecordsmanager.search.sql.ObjectSearchSchema;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.io.IOException;
import java.io.InputStream;
import java.util.*;
import java.util.stream.Collectors;

@Service
public class RecordService {

    private static final int SECURITY_OVERFETCH_FACTOR = 3;

    private final DataRepository repository;
    private final ConfigService config;
    private final ComponentCatalog catalog;
    private final ExpressionsService expressions;
    private final AuditService auditService;
    private final AuditPolicyService auditPolicyService;
    private final ObjectPropertyApplier propertyApplier;
    private final ObjectSearchExecutor searchExecutor;
    private final ObjectSearchSchema searchSchema;

    public RecordService(
            DataRepository repository,
            ConfigService config,
            ComponentCatalog catalog,
            ExpressionsService expressions,
            AuditService auditService,
            AuditPolicyService auditPolicyService,
            ObjectPropertyApplier propertyApplier,
            ObjectSearchExecutor searchExecutor,
            BuiltinColumnResolver columnResolver
    ) {
        this.repository = repository;
        this.config = config;
        this.catalog = catalog;
        this.expressions = expressions;
        this.auditService = auditService;
        this.auditPolicyService = auditPolicyService;
        this.propertyApplier = propertyApplier;
        this.searchExecutor = searchExecutor;
        this.searchSchema = ObjectSearchSchema.of(
                SearchFieldTarget.RECORD,
                Record.class,
                Record.BUILTIN_PROPERTY_BINDINGS,
                columnResolver
        );
    }

    @Transactional(readOnly = true)
    public RecordSearchResponse search(User actor, ObjectSearchRequest request) {
        int pageLimit = request.limitOrDefault();
        int fetchLimit = pageLimit * SECURITY_OVERFETCH_FACTOR;
        UUID cursor = request.cursor();

        List<RecordResponse> items = new ArrayList<>(pageLimit);

        while (items.size() < pageLimit) {
            List<UUID> candidateIds = this.searchExecutor.searchIds(
                    this.searchSchema,
                    actor,
                    request.q(),
                    request.filters(),
                    request.matchOrDefault(),
                    request.type(),
                    cursor,
                    fetchLimit
            );
            if (candidateIds.isEmpty()) {
                break;
            }

            Map<UUID, Record> loaded = this.repository.recordRepo.findAllById(candidateIds).stream()
                    .collect(Collectors.toMap(Record::getId, r -> r));

            for (UUID id : candidateIds) {
                Record record = loaded.get(id);
                if (record == null) {
                    continue;
                }
                if (!record.securityFilter(this.expressions, actor).canSeeMetadata()) {
                    continue;
                }
                items.add(RecordResponse.of(record));
                if (items.size() >= pageLimit) {
                    break;
                }
            }

            cursor = candidateIds.getLast();
            if (candidateIds.size() < fetchLimit || items.size() >= pageLimit) {
                break;
            }
        }

        UUID nextCursor = items.size() == pageLimit ? items.getLast().id() : null;

        String scope = request.type() != null ? request.type().toString() : AuditService.COLLECTION_TARGET_ID;
        this.auditService.recordSearchRead(
                AuditEntityType.RECORD,
                scope,
                this.searchExecutor.summarize(request.q(), request.filters()),
                items.size()
        );

        return new RecordSearchResponse(List.copyOf(items), nextCursor);
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

        List<AuditPropertyChange> changes = new ArrayList<>();
        changes.add(AuditPropertyChange.newProperty("type", input.type()));
        changes.add(AuditPropertyChange.newProperty("title", input.title()));

        Record record = new Record(input.title(), type);
        this.propertyApplier.applyOnCreate(record, input.properties(), false, changes);

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

        if (input.type() != null && !input.type().equals(record.getType().getId())) {
            RecordType newType = this.repository.recordTypeRepo.findById(input.type())
                    .orElseThrow(() -> new ResourceNotFoundException(ComponentTypes.RECORD_TYPE, input.type()));

            ResourceIdentifier oldType = record.getType().getId();
            record.setType(newType);
            changes.add(AuditPropertyChange.of("type", oldType, input.type()));
        }

        if (input.title() != null && !input.title().equals(record.getTitle())) {
            String oldTitle = record.getTitle();
            record.setTitle(input.title());
            changes.add(AuditPropertyChange.of("title", oldTitle, input.title()));
        }

        if (input.properties() != null) {
            this.propertyApplier.applyOnUpdate(record, input.properties(), false, changes);
        }

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
    public RecordResponse createRevision(User actor, UUID id, String version, String fileExtension, InputStream file) throws IOException {
        Record record = this.repository.recordRepo.findById(id)
                .orElseThrow(() -> new ResourceNotFoundException("record", id));

        // Security filtering on the record
        SecurityFilterUsage filter = record.securityFilter(this.expressions, actor);
        if (!filter.canSeeMetadata()) {
            throw new ResourceNotFoundException("record", id);
        }
        if (!filter.canSeeFiles()) {
            throw new ForbiddenException("record_upload");
        }

        if (!record.getType().supportsFile()) {
            throw new RecordTypeNoFileSupportException(record.getType().getId());
        }

        UUID defaultStoreId = this.config.getOptional(BuiltinConfigs.DEFAULT_FILE_STORE)
                .orElseThrow(DefaultFileStoreNotSetException::new);

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
                        AuditPropertyChange.newProperty("createdDate", rev.getDateCreated())
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
            throw new ActionNotAvailableException(actionId, "record", recordId);
        }

        this.auditPolicyService.validateCommentRequired(AuditEntityType.RECORD, AuditOperation.ACTION);

        parseAndExecute(action, context, inputs);

        this.auditService.addActionRanEvent(
                actionId,
                AuditEntityType.RECORD,
                recordId,
                Map.of("inputs", inputs.keySet())
        );
    }

    private static <I extends java.lang.Record> void parseAndExecute(
            RecordActionType<I> action,
            RecordActionContextImpl context,
            Map<String, ?> inputs
    ) {
        action.execute(context, JsonSchemaValidator.toRecord(action.getInputClass(), inputs));
    }
}
