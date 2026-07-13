package com.openrecordsmanager.record;

import com.openrecordsmanager.api.swagger.DefaultApiResponses;
import com.openrecordsmanager.api.swagger.NotFoundApiResponse;
import com.openrecordsmanager.record.dto.NewRecord;
import com.openrecordsmanager.record.dto.RecordResponse;
import com.openrecordsmanager.record.dto.RecordRevisionResponse;
import com.openrecordsmanager.user.User;
import io.swagger.v3.oas.annotations.Operation;
import jakarta.validation.constraints.Pattern;
import org.jspecify.annotations.Nullable;
import org.springframework.core.io.InputStreamResource;
import org.springframework.core.io.Resource;
import org.springframework.http.ContentDisposition;
import org.springframework.http.HttpHeaders;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.multipart.MultipartFile;

import java.io.IOException;
import java.io.InputStream;
import java.util.UUID;

@RestController
@RequestMapping("/api/records")
@DefaultApiResponses
@PreAuthorize("isAuthenticated()")
public class RecordController {

    private final RecordService service;

    public RecordController(RecordService service) {
        this.service = service;
    }

    @GetMapping(value = "/{id}", produces = MediaType.APPLICATION_JSON_VALUE)
    @Operation(summary = "Get record details")
    @NotFoundApiResponse
    public RecordResponse get(@AuthenticationPrincipal User user, @PathVariable("id") UUID id) {
        return this.service.get(user, id);
    }

    @PostMapping(produces = MediaType.APPLICATION_JSON_VALUE)
    @Operation(summary = "Create a new record")
    @NotFoundApiResponse
    public RecordResponse newRecord(@RequestBody NewRecord input) {
        return this.service.create(input);
    }

    @PutMapping(
            value = "/{id}/{version}",
            consumes = MediaType.APPLICATION_OCTET_STREAM_VALUE,
            produces = MediaType.APPLICATION_JSON_VALUE
    )
    @Operation(summary = "Upload new record revision")
    @NotFoundApiResponse
    public RecordResponse createRevision(
            @AuthenticationPrincipal User user,
            @PathVariable("id") UUID id,
            @Pattern(regexp = "^[0-9]+(\\.[0-9]+)*$", message = "Version path parameter must be numbers and decimals only")
            @PathVariable("version") String version,
            @RequestHeader(value = HttpHeaders.CONTENT_DISPOSITION, required = false) @Nullable String dispositionHeader,
            @RequestParam(value = "ext", required = false, defaultValue = "") String fileExtension,
            InputStream file
    ) {
        // if no explicit stream extension is supplied, attempt to extract it from the content disposition header
        if (fileExtension.isBlank() && dispositionHeader != null) {
            ContentDisposition disposition = ContentDisposition.parse(dispositionHeader);
            String filename = disposition.getFilename();
            if (filename != null && !filename.isBlank()) {
                fileExtension = getExtensionFromFile(filename);
            }
        }

        return this.service.createRevision(user, id, version, fileExtension, file);
    }

    @PutMapping(
            value = "/{id}/{version}",
            consumes = MediaType.MULTIPART_FORM_DATA_VALUE,
            produces = MediaType.APPLICATION_JSON_VALUE
    )
    @Operation(summary = "Upload new record revision")
    @NotFoundApiResponse
    public RecordResponse createRevision2(
            @AuthenticationPrincipal User user,
            @PathVariable("id") UUID id,
            @Pattern(regexp = "^[0-9]+(\\.[0-9]+)*$", message = "Version path parameter must be numbers and decimals only")
            @PathVariable("version") String version,
            @RequestPart("stream") MultipartFile file
    ) throws IOException {
        String extension = "";
        if (file.getOriginalFilename() != null && file.getOriginalFilename().contains(".")) {
            extension = getExtensionFromFile(file.getOriginalFilename());
        }

        return this.service.createRevision(user, id, version, extension, file.getInputStream());
    }

    private String getExtensionFromFile(String fileName) {
        return fileName.substring(fileName.lastIndexOf("."));
    }

    @GetMapping(value = "/{id}/{version}", produces = MediaType.APPLICATION_OCTET_STREAM_VALUE)
    @Operation(summary = "Get record revision file")
    @NotFoundApiResponse
    public ResponseEntity<Resource> getRevision(
            @AuthenticationPrincipal User user,
            @PathVariable("id") UUID id,
            @Pattern(regexp = "^[0-9]+(\\.[0-9]+)*$", message = "Version path parameter must be numbers and decimals only")
            @PathVariable("version") String version
    ) {
        RecordRevisionResponse rev = this.service.getRevision(user, id, version);

        InputStreamResource streamResource = new InputStreamResource(rev.stream()) {
            @Override
            public long contentLength() {
                return rev.sizeBytes();
            }
        };

        return ResponseEntity.ok()
                .headers(headers -> {
                    headers.setContentDisposition(rev.dispositionHeader());
                    headers.add("Content-Digest", rev.digestHeader());
                })
                .contentLength(rev.sizeBytes())
                .contentType(MediaType.APPLICATION_OCTET_STREAM)
                .body(streamResource);
    }
}
