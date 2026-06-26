package com.openrecordsmanager.controllers;

import com.openrecordsmanager.model.repositories.DataRepository;
import com.openrecordsmanager.resources.ComponentCatalog;
import com.openrecordsmanager.resources.ExpressionsService;
import com.openrecordsmanager.resources.ResourceIdentifier;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import java.util.Map;
import java.util.Set;

@RestController
@RequestMapping("/records")
public class RecordController {

    private final ComponentCatalog registry;
    private final ExpressionsService expressions;
    private final DataRepository repository;

    public RecordController(ComponentCatalog registry, ExpressionsService expressions, DataRepository repository) {
        this.registry = registry;
        this.expressions = expressions;
        this.repository = repository;
    }

    @PostMapping()
    public ApiResponse<Set<ResourceIdentifier>> newRecord(@RequestBody NewRecordContent data) {
        System.out.println(data.type);
        System.out.println(data.values);


        throw new UnsupportedOperationException("Not supported yet.");
    }

    public record NewRecordContent(
            ResourceIdentifier type,
            Map<ResourceIdentifier, Object> values
    ) {
    }


}
