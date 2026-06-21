package com.openrecordsmanager.controllers;

import com.openrecordsmanager.model.ObjectProperty;
import com.openrecordsmanager.model.RecordType;
import com.openrecordsmanager.model.repositories.ListTypeRepository;
import com.openrecordsmanager.model.repositories.RecordPropertyRepository;
import com.openrecordsmanager.model.repositories.RecordTypeRepository;
import com.openrecordsmanager.property.PropertyDefinition;
import com.openrecordsmanager.recordtype.RecordTypeDefinition;
import com.openrecordsmanager.resources.ResourceIdentifier;
import com.openrecordsmanager.resources.ResourceRegistry;
import com.openrecordsmanager.resources.ResourceType;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.util.Optional;
import java.util.Set;
import java.util.stream.Collectors;

@RestController
@RequestMapping("/record")
public class RecordController {

    private final ResourceRegistry resourceRegistry;
    private final RecordTypeRepository recordTypeRepository;
    private final RecordPropertyRepository recordPropertyRepository;
    private final ListTypeRepository listTypeRepository;

    public RecordController(ResourceRegistry resourceRegistry, RecordTypeRepository recordTypeRepository, RecordPropertyRepository recordPropertyRepository, ListTypeRepository listTypeRepository) {
        this.resourceRegistry = resourceRegistry;
        this.recordTypeRepository = recordTypeRepository;
        this.recordPropertyRepository = recordPropertyRepository;
        this.listTypeRepository = listTypeRepository;
    }

    @GetMapping("/type")
    public ResponseEntity<ApiResponse<Set<ResourceIdentifier>>> getRecordTypes() {
        return ResponseEntity.ok(ApiResponse.success(this.recordTypeRepository.findAll().stream().map(listType -> listType.id).collect(Collectors.toSet())));
    }

    @GetMapping("/type/{type}")
    public ResponseEntity<ApiResponse<RecordType>> getRecordType(@PathVariable("type") ResourceIdentifier typeId) {
        Optional<RecordType> recTemplate = this.recordTypeRepository.findById(typeId);
        if (recTemplate.isEmpty()) {
            return ResponseEntity.status(HttpStatus.NOT_FOUND).body(ApiResponse.error("record_template_not_found"));
        }
        return ResponseEntity.ok(ApiResponse.success(recTemplate.get()));
    }

    @GetMapping("/type/template")
    public ResponseEntity<ApiResponse<Set<ResourceIdentifier>>> getRecordTypeTemplates() {
        return ResponseEntity.ok(ApiResponse.success(this.resourceRegistry.getIds(ResourceType.RECORD_TYPE)));
    }

    @GetMapping("/type/template/{template}")
    public ResponseEntity<ApiResponse<RecordTypeDefinition>> getRecordTypeTemplate(@PathVariable("template") ResourceIdentifier templateId) {
        RecordTypeDefinition template = this.resourceRegistry.getComponent(ResourceType.RECORD_TYPE, templateId);
        if (template == null) {
            return ResponseEntity.status(HttpStatus.NOT_FOUND).body(ApiResponse.error("record_template_not_found"));
        }
        return ResponseEntity.ok(ApiResponse.success(template));
    }

    @PostMapping("/type/template/{template}/apply")
    public ResponseEntity<ApiResponse<RecordType>> applyRecordTypeTemplate(@PathVariable("template") ResourceIdentifier templateId, @RequestParam(value = "makeProperties", required = false, defaultValue = "false") boolean makeProperties) {
        RecordTypeDefinition template = this.resourceRegistry.getComponent(ResourceType.RECORD_TYPE, templateId);
        if (template == null) {
            return ResponseEntity.status(HttpStatus.NOT_FOUND).body(ApiResponse.error("record_template_not_found"));
        }

        if (makeProperties) {
            for (PropertyDefinition<?> property : template.properties()) {
                ResourceIdentifier id = this.resourceRegistry.getResourceId(ResourceType.PROPERTY, property);
                Optional<ObjectProperty<?>> prop = this.recordPropertyRepository.findById(id);
                if (prop.isEmpty()) {
                    constructProperty(id, property);
                }
            }
        }

        // Validate all properties exist
        for (PropertyDefinition<?> property : template.properties()) {
            ResourceIdentifier id = this.resourceRegistry.getResourceId(ResourceType.PROPERTY, property);
            Optional<ObjectProperty<?>> prop = this.recordPropertyRepository.findById(id);
            if (prop.isEmpty()) {
                return ResponseEntity.status(HttpStatus.NOT_FOUND).body(ApiResponse.error("record_property_not_found"));
            }
        }

        RecordType type = this.recordTypeRepository.findById(templateId).orElse(new RecordType(templateId, this.resourceRegistry, template));
        this.recordTypeRepository.saveAndFlush(type);

        return ResponseEntity.ok(ApiResponse.success(type));
    }

    private <T> void constructProperty(ResourceIdentifier id, PropertyDefinition<T> definition) {
        ObjectProperty<T> objectProperty = new ObjectProperty<T>(id, this.listTypeRepository, this.resourceRegistry, definition);
        this.recordPropertyRepository.saveAndFlush(objectProperty);
    }

    @PutMapping("/")
    public ResponseEntity<ApiResponse<Set<ResourceIdentifier>>> putRecord() {
        throw new UnsupportedOperationException("Not supported yet.");
    }


}
