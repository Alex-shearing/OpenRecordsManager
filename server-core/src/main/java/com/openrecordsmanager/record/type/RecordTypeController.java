package com.openrecordsmanager.record.type;

import com.openrecordsmanager.api.ResourceIdentifier;
import com.openrecordsmanager.record.type.dto.NewRecordTypeRequest;
import com.openrecordsmanager.record.type.dto.RecordTypeResponse;
import com.openrecordsmanager.record.type.dto.UpdateRecordTypeRequest;
import com.openrecordsmanager.rest.swagger.ConflictApiResponse;
import com.openrecordsmanager.rest.swagger.DefaultApiResponses;
import com.openrecordsmanager.rest.swagger.NotFoundApiResponse;
import io.swagger.v3.oas.annotations.Operation;
import org.springframework.http.MediaType;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.*;

import java.util.List;

@RestController
@RequestMapping("/api/record_types")
@DefaultApiResponses
@PreAuthorize("isAuthenticated()")
public class RecordTypeController {

    private final RecordTypeService service;

    public RecordTypeController(RecordTypeService service) {
        this.service = service;
    }

    @GetMapping(produces = MediaType.APPLICATION_JSON_VALUE)
    @Operation(summary = "List all record types")
    public List<RecordTypeResponse> getRecordTypes() {
        return this.service.getAll();
    }

    @GetMapping(value = "/{id}", produces = MediaType.APPLICATION_JSON_VALUE)
    @Operation(summary = "Get record type details")
    @NotFoundApiResponse
    public RecordTypeResponse getRecordType(@PathVariable("id") ResourceIdentifier id) {
        return this.service.get(id);
    }

    @PostMapping(consumes = MediaType.APPLICATION_JSON_VALUE, produces = MediaType.APPLICATION_JSON_VALUE)
    @Operation(summary = "Create a custom record type")
    @ConflictApiResponse
    public RecordTypeResponse createRecordType(@RequestBody NewRecordTypeRequest input) {
        return this.service.create(input);
    }

    @PutMapping(value = "/{id}", consumes = MediaType.APPLICATION_JSON_VALUE, produces = MediaType.APPLICATION_JSON_VALUE)
    @Operation(summary = "Update a custom record type")
    @NotFoundApiResponse
    public RecordTypeResponse updateRecordType(
            @PathVariable("id") ResourceIdentifier id,
            @RequestBody UpdateRecordTypeRequest input
    ) {
        return this.service.update(id, input);
    }
}
