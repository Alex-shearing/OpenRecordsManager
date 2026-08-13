package com.openrecordsmanager.record;

import com.openrecordsmanager.api.builtin.BuiltinConfigs;
import com.openrecordsmanager.api.errors.ResourceNotFoundException;
import com.openrecordsmanager.api.types.ComponentTypes;
import com.openrecordsmanager.config.ConfigService;
import com.openrecordsmanager.database.DataRepository;
import com.openrecordsmanager.filestore.store.FileStore;
import com.openrecordsmanager.plugin.ExpressionsService;
import com.openrecordsmanager.plugin.registry.ComponentCatalog;
import com.openrecordsmanager.property.ObjectProperty;
import com.openrecordsmanager.record.dto.NewRecord;
import com.openrecordsmanager.record.dto.RecordResponse;
import com.openrecordsmanager.record.dto.RecordRevisionResponse;
import com.openrecordsmanager.recordtype.RecordType;
import com.openrecordsmanager.user.User;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.io.InputStream;
import java.text.MessageFormat;
import java.util.Objects;
import java.util.UUID;

@Service
public class RecordService {

    private final DataRepository repository;
    private final ConfigService config;
    private final ComponentCatalog catalog;
    private final ExpressionsService expressions;

    public RecordService(
            DataRepository repository,
            ConfigService config,
            ComponentCatalog catalog,
            ExpressionsService expressions
    ) {
        this.repository = repository;
        this.config = config;
        this.catalog = catalog;
        this.expressions = expressions;
    }

    @Transactional(readOnly = true)
    public RecordResponse get(User user, UUID id) {
        return this.repository.recordRepo.findById(id)
                .filter(record -> record.securityFilter(this.expressions, user, record).canSeeMetadata())
                .map(RecordResponse::from)
                .orElseThrow(() -> new ResourceNotFoundException("record", id));
    }

    @Transactional
    public RecordResponse create(NewRecord input) {
        RecordType type = this.repository.recordTypeRepo.findById(input.type())
                .orElseThrow(() -> new ResourceNotFoundException(ComponentTypes.RECORD_TYPE, input.type()));

        Record record = new Record("tba", type);
        input.properties().forEach((identifier, o) -> {
            ObjectProperty<?> property = this.repository.objectPropertyRepo.findById(identifier)
                    .orElseThrow(() -> new ResourceNotFoundException(ComponentTypes.OBJECT_PROPERTY, identifier));

            setProperty(record, property, o);
        });

        this.repository.recordRepo.saveAndFlush(record);

        return RecordResponse.from(record);
    }

    private static <K> void setProperty(Record record, ObjectProperty<K> property, Object value) {
        record.setProperty(property, Objects.requireNonNull(property.getType().cast(value)));
    }

    @Transactional
    public RecordResponse createRevision(User user, UUID id, String version, String fileExtension, InputStream file) {
        Record record = this.repository.recordRepo.findById(id)
                .filter(r -> r.securityFilter(this.expressions, user, r).canSeeFiles())
                .orElseThrow(() -> new ResourceNotFoundException("record", id));

        if (!record.getType().supportsFile()) {
            throw new IllegalArgumentException(MessageFormat.format("Record type {0} does not support attaching a file", record.getType().id));
        }

        UUID defaultStoreId = this.config.getOptional(BuiltinConfigs.DEFAULT_FILE_STORE)
                .orElseThrow(() -> new IllegalStateException("There is no default file store set"));

        FileStore<?> fileStore = this.repository.fileStoreRepo.findById(defaultStoreId)
                .orElseThrow(() -> new ResourceNotFoundException("file store", defaultStoreId));

        record.addRevision(version, fileStore.newFile(this.catalog, file, fileExtension));

        return RecordResponse.from(this.repository.recordRepo.saveAndFlush(record));
    }

    @Transactional(readOnly = true)
    public RecordRevisionResponse getRevision(User user, UUID id, String version) {
        RecordRevision rev = this.repository.recordRepo.findByRecordId(id, version)
                .filter(r -> r.record.securityFilter(this.expressions, user, r.record).canSeeFiles())
                .orElseThrow(() -> new ResourceNotFoundException("record revision", id + "/" + version));

        return new RecordRevisionResponse(
                rev.file.getFile(this.catalog),
                rev.version,
                rev.file.extension,
                rev.file.sizeBytes,
                rev.file.hash,
                rev.file.hashAlgorithm
        );
    }
}
