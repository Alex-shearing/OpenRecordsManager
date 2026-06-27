package com.openrecordsmanager.controllers;

import com.openrecordsmanager.controllers.errors.ApiError;
import com.openrecordsmanager.model.RecordType;
import com.openrecordsmanager.model.repositories.DataRepository;
import com.openrecordsmanager.resources.ExpressionsService;
import com.openrecordsmanager.resources.ResourceIdentifier;
import com.openrecordsmanager.resources.types.ComponentTypes;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import java.util.List;

@RestController
@RequestMapping("/record_types")
public class RecordTypeController {

    private final ExpressionsService expressions;
    private final DataRepository repository;

    public RecordTypeController(ExpressionsService expressions, DataRepository repository) {
        this.expressions = expressions;
        this.repository = repository;
    }

    @GetMapping
    public List<ResourceIdentifier> getRecordTypes() {
        return this.repository.recordTypeRepo.findAllIds();
    }

    @GetMapping("/{id}")
    public RecordType getRecordType(@PathVariable("id") ResourceIdentifier id) {
        return this.repository.recordTypeRepo.findById(id)
                .orElseThrow(() -> ApiError.notFound(ComponentTypes.RECORD_TYPE, id));
    }

}
