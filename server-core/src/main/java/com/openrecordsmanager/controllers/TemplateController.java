package com.openrecordsmanager.controllers;

import com.openrecordsmanager.api.list.ListDefinition;
import com.openrecordsmanager.api.recordtype.RecordTypeDefinition;
import com.openrecordsmanager.controllers.errors.ApiError;
import com.openrecordsmanager.model.ListType;
import com.openrecordsmanager.model.RecordType;
import com.openrecordsmanager.model.repositories.DataRepository;
import com.openrecordsmanager.resources.ComponentCatalog;
import com.openrecordsmanager.resources.ExpressionsService;
import com.openrecordsmanager.resources.ResourceIdentifier;
import com.openrecordsmanager.resources.types.ComponentTypes;
import org.springframework.web.bind.annotation.*;

import java.util.Set;

@RestController
@RequestMapping("/templates")
public class TemplateController {

    private final ComponentCatalog catalog;
    private final ExpressionsService expressions;
    private final DataRepository repository;

    public TemplateController(ComponentCatalog catalog, ExpressionsService expressions, DataRepository repository) {
        this.catalog = catalog;
        this.expressions = expressions;
        this.repository = repository;
    }

    @GetMapping("/record_types")
    public ApiResponse<Set<ResourceIdentifier>> getRecordTypeTemplates() {
        return ApiResponse.success(this.catalog.getIds(ComponentTypes.RECORD_TYPE));
    }

    @GetMapping("/record_types/{template}")
    public ApiResponse<RecordTypeDefinition> getRecordTypeTemplate(@PathVariable("template") ResourceIdentifier templateId) {
        RecordTypeDefinition template = ComponentTypes.RECORD_TYPE.getComponent(templateId, this.catalog)
                .orElseThrow(() -> ApiError.notFound("record template", templateId));

        return ApiResponse.success(template);
    }

    @PostMapping("/record_types/{template}/apply")
    public ApiResponse<RecordType> applyRecordTypeTemplate(@PathVariable("template") ResourceIdentifier templateId, @RequestParam(value = "includeDependencies", required = false, defaultValue = "false") boolean includeDependencies) {
        RecordTypeDefinition template = ComponentTypes.RECORD_TYPE.getComponent(templateId, this.catalog)
                .orElseThrow(() -> ApiError.notFound("record template", templateId));

        RecordType type = ComponentTypes.RECORD_TYPE.register(this.repository, this.catalog, this.expressions, templateId, template, includeDependencies);

        return ApiResponse.success(type);
    }

    @GetMapping("/lists")
    public ApiResponse<Set<ResourceIdentifier>> getTemplates() {
        return ApiResponse.success(this.catalog.getIds(ComponentTypes.LIST));
    }

    @GetMapping("/lists/{template}")
    public ApiResponse<ListDefinition> getTemplate(@PathVariable("template") ResourceIdentifier templateId) {
        ListDefinition listDef = ComponentTypes.LIST.getComponent(templateId, this.catalog)
                .orElseThrow(() -> ApiError.notFound("list template", templateId));

        return ApiResponse.success(listDef);
    }

    @PostMapping("/lists/{template}/apply")
    public ApiResponse<ListType> applyList(@PathVariable("template") ResourceIdentifier templateId) {
        ListDefinition listDef = ComponentTypes.LIST.getComponent(templateId, this.catalog)
                .orElseThrow(() -> ApiError.notFound("list template", templateId));

        ListType type = ComponentTypes.LIST.register(this.repository, this.catalog, this.expressions, templateId, listDef, false);

        listDef.defaultEntries.forEach((s, listItem) -> {
            ResourceIdentifier id = new ResourceIdentifier(templateId.source(), s);
            ComponentTypes.LIST_ELEMENT.register(this.repository, this.catalog, this.expressions, id, listItem, false);
        });

        return ApiResponse.success(type);
    }

}
