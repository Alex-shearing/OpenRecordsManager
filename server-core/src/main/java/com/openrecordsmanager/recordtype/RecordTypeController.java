package com.openrecordsmanager.recordtype;

import com.openrecordsmanager.api.ResourceIdentifier;
import com.openrecordsmanager.api.errors.ApiError;
import com.openrecordsmanager.api.swagger.DefaultErrorResponses;
import com.openrecordsmanager.api.swagger.NotFoundApiResponse;
import com.openrecordsmanager.api.types.ComponentTypes;
import com.openrecordsmanager.database.DataRepository;
import com.openrecordsmanager.plugin.ExpressionsService;
import io.swagger.v3.oas.annotations.Operation;
import org.springframework.http.MediaType;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import java.util.List;

@RestController
@RequestMapping("/api/record_types")
@DefaultErrorResponses
@PreAuthorize("isAuthenticated()")
public class RecordTypeController {

    private final ExpressionsService expressions;
    private final DataRepository repository;

    public RecordTypeController(ExpressionsService expressions, DataRepository repository) {
        this.expressions = expressions;
        this.repository = repository;
    }

    @GetMapping(produces = MediaType.APPLICATION_JSON_VALUE)
    @Operation(summary = "List all record types")
    @Transactional(readOnly = true)
    public List<ResourceIdentifier> getRecordTypes() {
        return this.repository.recordTypeRepo.findAllIds();
    }

    @GetMapping(value = "/{id}", produces = MediaType.APPLICATION_JSON_VALUE)
    @Operation(summary = "Get record type details")
    @NotFoundApiResponse
    @Transactional(readOnly = true)
    public RecordType getRecordType(@PathVariable("id") ResourceIdentifier id) {
        return this.repository.recordTypeRepo.findById(id)
                .orElseThrow(() -> ApiError.notFound(ComponentTypes.RECORD_TYPE, id));
    }

}
