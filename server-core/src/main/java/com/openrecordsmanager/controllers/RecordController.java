package com.openrecordsmanager.controllers;

import com.openrecordsmanager.api.ResourceIdentifier;
import com.openrecordsmanager.api.types.ComponentTypes;
import com.openrecordsmanager.config.ConfigProperties;
import com.openrecordsmanager.config.DynamicConfigService;
import com.openrecordsmanager.controllers.repsonse.InternalServerErrorApiResponse;
import com.openrecordsmanager.controllers.repsonse.NotFoundApiResponse;
import com.openrecordsmanager.controllers.repsonse.errors.ApiError;
import com.openrecordsmanager.model.*;
import com.openrecordsmanager.model.Record;
import com.openrecordsmanager.model.repositories.DataRepository;
import com.openrecordsmanager.resources.ComponentCatalog;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.responses.ApiResponse;
import org.springframework.core.io.Resource;
import org.springframework.http.ContentDisposition;
import org.springframework.http.HttpHeaders;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.multipart.MultipartFile;

import java.io.IOException;
import java.io.InputStream;
import java.util.Locale;
import java.util.Map;
import java.util.UUID;

@RestController
@RequestMapping("/records")
@InternalServerErrorApiResponse
@ApiResponse(responseCode = "200")
public class RecordController {

    private final DataRepository repository;
    private final DynamicConfigService config;
    private final ComponentCatalog catalog;

    public RecordController(DataRepository repository, DynamicConfigService config, ComponentCatalog catalog) {
        this.repository = repository;
        this.config = config;
        this.catalog = catalog;
    }

    @PostMapping(produces = MediaType.APPLICATION_JSON_VALUE)
    @NotFoundApiResponse
    @Operation(summary = "Create a new record")
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

    @GetMapping(value = "/{id}", produces = MediaType.APPLICATION_JSON_VALUE)
    @Operation(summary = "Get record details")
    @NotFoundApiResponse
    public Record getRecord(@PathVariable("id") UUID id) {
        return this.repository.recordRepo.findById(id)
                .orElseThrow(() -> ApiError.notFound("record", id.toString()));
    }

    @PutMapping(
            value = "/{id}/revisions/{version}",
            consumes = MediaType.APPLICATION_OCTET_STREAM_VALUE,
            produces = MediaType.APPLICATION_JSON_VALUE
    )
    @Operation(summary = "Upload new record revision")
    @NotFoundApiResponse
    public void newRevision(
            @PathVariable("id") UUID id,
            @PathVariable("version") double version,
            @RequestHeader(value = HttpHeaders.CONTENT_DISPOSITION, required = false) String dispositionHeader,
            @RequestParam(value = "ext", required = false, defaultValue = "") String fileExtension,
            InputStream file
    ) {
        Record record = this.repository.recordRepo.findById(id)
                .orElseThrow(() -> ApiError.notFound("record", id.toString()));

        UUID defaultStoreId = this.config.getProperty(ConfigProperties.WORKGROUP_DEFAULT_FILE_STORE);
        if (defaultStoreId == null) {
            throw ApiError.serverError("There is no value set for the {0} configuration", ConfigProperties.WORKGROUP_DEFAULT_FILE_STORE.key());
        }

        FileStore<?> fileStore = this.repository.fileStoreRepo.findById(defaultStoreId)
                .orElseThrow(() -> ApiError.notFound("file store", defaultStoreId.toString()));

        // if no explicit file extension is supplied, attempt to extract it from the content disposition header
        if (fileExtension.isBlank() && dispositionHeader != null) {
            ContentDisposition disposition = ContentDisposition.parse(dispositionHeader);
            String filename = disposition.getFilename();
            if (filename != null && !filename.isBlank()) {
                fileExtension = getExtensionFromFile(filename);
            }
        }

        record.addRevision(version, fileStore.newFile(this.catalog, file, fileExtension));

        this.repository.recordRepo.saveAndFlush(record);
    }

    @PutMapping(
            value = "/{id}/revisions/{version}",
            consumes = MediaType.MULTIPART_FORM_DATA_VALUE,
            produces = MediaType.APPLICATION_JSON_VALUE
    )
    @Operation(summary = "Upload new record revision")
    @NotFoundApiResponse
    public void newRevision(
            @PathVariable("id") UUID id,
            @PathVariable("version") double version,
            @RequestPart("file") MultipartFile file
    ) throws IOException {
        String extension = "";
        if (file.getOriginalFilename() != null && file.getOriginalFilename().contains(".")) {
            extension = getExtensionFromFile(file.getOriginalFilename());
        }

        this.newRevision(id, version, null, extension, file.getInputStream());
    }

    private String getExtensionFromFile(String fileName) {
        return fileName.substring(fileName.lastIndexOf("."));
    }

    @GetMapping(value = "/{id}/revisions", produces = MediaType.APPLICATION_JSON_VALUE)
    @Operation(summary = "List all revisions for a record")
    @NotFoundApiResponse
    public double[] getRevisions(@PathVariable("id") UUID id) {
        return this.repository.recordRepo.getRevisions(id)
                .orElseThrow(() -> ApiError.notFound("record", id.toString()));
    }

    @GetMapping(value = "/{id}/revisions/{version}", produces = MediaType.APPLICATION_OCTET_STREAM_VALUE)
    @Operation(summary = "Get record revision file")
    @NotFoundApiResponse
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
