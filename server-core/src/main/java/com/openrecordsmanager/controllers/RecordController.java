package com.openrecordsmanager.controllers;

import com.openrecordsmanager.config.ConfigProperties;
import com.openrecordsmanager.controllers.errors.ApiError;
import com.openrecordsmanager.model.*;
import com.openrecordsmanager.model.Record;
import com.openrecordsmanager.model.repositories.DataRepository;
import com.openrecordsmanager.resources.ComponentCatalog;
import com.openrecordsmanager.resources.ExpressionsService;
import com.openrecordsmanager.resources.ResourceIdentifier;
import com.openrecordsmanager.resources.types.ComponentTypes;
import org.springframework.core.env.Environment;
import org.springframework.core.io.Resource;
import org.springframework.http.HttpHeaders;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.io.InputStream;
import java.util.List;
import java.util.Locale;
import java.util.Map;
import java.util.UUID;
import java.util.stream.Collectors;

@RestController
@RequestMapping("/records")
public class RecordController {

    private final ExpressionsService expressions;
    private final DataRepository repository;
    private final Environment environment;
    private final ComponentCatalog catalog;

    public RecordController(ExpressionsService expressions, DataRepository repository, Environment environment, ComponentCatalog catalog) {
        this.expressions = expressions;
        this.repository = repository;
        this.environment = environment;
        this.catalog = catalog;
    }

    @PostMapping()
    public Record newRecord(@RequestBody NewRecordContent input) {
        RecordType type = this.repository.recordTypeRepo.findById(input.type())
                .orElseThrow(() -> ApiError.notFound(ComponentTypes.RECORD_TYPE, input.type()));

        Record record = new Record("tba", type);
        input.properties.forEach((identifier, o) -> {
            ObjectProperty<?> property = this.repository.objectPropertyRepo.findById(identifier)
                    .orElseThrow(() -> ApiError.notFound(ComponentTypes.PROPERTY, identifier));

            setProperty(record, property, o);
        });

        return this.repository.recordRepo.saveAndFlush(record);
    }

    @GetMapping("/{id}")
    public Record getRecord(@PathVariable("id") UUID id) {
        return this.repository.recordRepo.findById(id)
                .orElseThrow(() -> ApiError.notFound("record", id.toString()));
    }

    @PutMapping(value = "/{id}/revisions/{version}", consumes = MediaType.APPLICATION_OCTET_STREAM_VALUE)
    public void newRevision(@PathVariable("id") UUID id, @PathVariable("version") double version, InputStream file) {
        Record record = this.repository.recordRepo.findById(id)
                .orElseThrow(() -> ApiError.notFound("record", id.toString()));

        UUID defaultStoreId = this.environment.getProperty(ConfigProperties.WORKGROUP_DEFAULT_FILE_STORE.id(), UUID.class);
        if (defaultStoreId == null) {
            throw ApiError.serverError("There is no value set for the {0} configuration", ConfigProperties.WORKGROUP_DEFAULT_FILE_STORE.id());
        }

        FileStore fileStore = this.repository.fileStoreRepo.findById(defaultStoreId)
                .orElseThrow(() -> ApiError.notFound("file store", id.toString()));

        record.addRevision(version, fileStore.newFile(file, this.catalog));

        this.repository.recordRepo.saveAndFlush(record);
    }

    @GetMapping("/{id}/revisions")
    public List<Double> getRevisions(@PathVariable("id") UUID id) {
        return this.repository.recordRepo.findById(id)
                .orElseThrow(() -> ApiError.notFound("record", id.toString()))
                .revisions.stream().map(recordRevision -> recordRevision.version).collect(Collectors.toList());
    }

    @GetMapping("/{id}/revisions/{version}")
    public ResponseEntity<Resource> getRevision(@PathVariable("id") UUID id, @PathVariable("version") double version) {
        RecordRevision rev = this.repository.recordRepo.findByRecordId(id, version)
                .orElseThrow(() -> ApiError.notFound("record revision", id.toString() + "/" + version));

        HttpHeaders headers = new HttpHeaders();
        headers.add(HttpHeaders.CONTENT_DISPOSITION, String.format("attachment; filename=\"%s\"", rev.file.getFileName(Double.toString(version))));
        headers.add("Content-Digest", String.format("%s=:%s:", rev.file.hashAlgorithm.toLowerCase(Locale.ROOT), rev.file.hash));

        return ResponseEntity.ok()
                .headers(headers)
                .contentLength(rev.file.sizeBytes)
                .contentType(MediaType.APPLICATION_OCTET_STREAM)
                .body(rev.file.getFile(this.catalog));
    }


    private static <K> void setProperty(Record record, ObjectProperty<K> property, Object value) {
        record.setProperty(property, property.type.cast(value));
    }

    public record NewRecordContent(
            ResourceIdentifier type,
            Map<ResourceIdentifier, Object> properties
    ) {
    }


}
