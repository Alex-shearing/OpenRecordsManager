package com.openrecordsmanager.recordtype;

import com.openrecordsmanager.api.ResourceIdentifier;
import com.openrecordsmanager.recordtype.dto.RecordTypeResponse;
import com.openrecordsmanager.rest.swagger.DefaultApiResponses;
import com.openrecordsmanager.rest.swagger.NotFoundApiResponse;
import io.swagger.v3.oas.annotations.Operation;
import org.springframework.http.MediaType;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

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
    public List<ResourceIdentifier> getRecordTypes() {
        return this.service.getAllIds();
    }

    @GetMapping(value = "/{id}", produces = MediaType.APPLICATION_JSON_VALUE)
    @Operation(summary = "Get record type details")
    @NotFoundApiResponse
    public RecordTypeResponse getRecordType(@PathVariable("id") ResourceIdentifier id) {
        return this.service.get(id);
    }
}
