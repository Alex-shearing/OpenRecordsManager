package com.openrecordsmanager.controllers;

import com.openrecordsmanager.api.recordtype.RecordTypeDefinition;
import com.openrecordsmanager.model.RecordType;
import com.openrecordsmanager.model.repositories.DataRepository;
import com.openrecordsmanager.resources.ExpressionsService;
import com.openrecordsmanager.resources.ResourceCatalog;
import com.openrecordsmanager.resources.ResourceIdentifier;
import com.openrecordsmanager.resources.types.ResourceTypes;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.util.Optional;
import java.util.Set;
import java.util.stream.Collectors;

@RestController
@RequestMapping("/record")
public class RecordController {

    private final ResourceCatalog registry;
    private final ExpressionsService expressions;
    private final DataRepository repository;

    public RecordController(ResourceCatalog registry, ExpressionsService expressions, DataRepository repository) {
        this.registry = registry;
        this.expressions = expressions;
        this.repository = repository;
    }

    @GetMapping("/type")
    public ResponseEntity<ApiResponse<Set<ResourceIdentifier>>> getRecordTypes() {
        return ResponseEntity.ok(ApiResponse.success(this.repository.recordTypeRepo.findAll().stream().map(listType -> listType.id).collect(Collectors.toSet())));
    }

    @GetMapping("/type/{type}")
    public ResponseEntity<ApiResponse<RecordType>> getRecordType(@PathVariable("type") ResourceIdentifier typeId) {
        Optional<RecordType> recTemplate = this.repository.recordTypeRepo.findById(typeId);
        if (recTemplate.isEmpty()) {
            return ResponseEntity.status(HttpStatus.NOT_FOUND).body(ApiResponse.error("record_template_not_found"));
        }
        return ResponseEntity.ok(ApiResponse.success(recTemplate.get()));
    }

    @GetMapping("/type/template")
    public ResponseEntity<ApiResponse<Set<ResourceIdentifier>>> getRecordTypeTemplates() {
        return ResponseEntity.ok(ApiResponse.success(this.registry.getIds(ResourceTypes.RECORD_TYPE)));
    }

    @GetMapping("/type/template/{template}")
    public ResponseEntity<ApiResponse<RecordTypeDefinition>> getRecordTypeTemplate(@PathVariable("template") ResourceIdentifier templateId) {
        RecordTypeDefinition template = this.registry.getComponent(ResourceTypes.RECORD_TYPE, templateId);
        if (template == null) {
            return ResponseEntity.status(HttpStatus.NOT_FOUND).body(ApiResponse.error("record_template_not_found"));
        }
        return ResponseEntity.ok(ApiResponse.success(template));
    }

    @PostMapping("/type/template/{template}/apply")
    public ResponseEntity<ApiResponse<RecordType>> applyRecordTypeTemplate(@PathVariable("template") ResourceIdentifier templateId, @RequestParam(value = "includeDependencies", required = false, defaultValue = "false") boolean includeDependencies) {
        RecordTypeDefinition template = this.registry.getComponent(ResourceTypes.RECORD_TYPE, templateId);
        if (template == null) {
            return ResponseEntity.status(HttpStatus.NOT_FOUND).body(ApiResponse.error("record_template_not_found"));
        }

        RecordType type = ResourceTypes.RECORD_TYPE.register(this.repository, this.registry, this.expressions, templateId, template, includeDependencies);

        return ResponseEntity.ok(ApiResponse.success(type));
    }

    @PutMapping("/")
    public ResponseEntity<ApiResponse<Set<ResourceIdentifier>>> putRecord() {
        throw new UnsupportedOperationException("Not supported yet.");
    }


}
