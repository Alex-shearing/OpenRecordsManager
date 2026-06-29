package com.openrecordsmanager.controllers;

import com.openrecordsmanager.api.list.ListDefinition;
import com.openrecordsmanager.api.recordtype.RecordTypeDefinition;
import com.openrecordsmanager.controllers.repsonse.errors.ApiError;
import com.openrecordsmanager.model.ListType;
import com.openrecordsmanager.model.RecordType;
import com.openrecordsmanager.model.repositories.DataRepository;
import com.openrecordsmanager.resources.ComponentCatalog;
import com.openrecordsmanager.resources.ExpressionsService;
import com.openrecordsmanager.resources.ResourceIdentifier;
import com.openrecordsmanager.resources.types.ComponentTypes;
import org.springframework.http.MediaType;
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

    @GetMapping(value = "/record_types", produces = MediaType.APPLICATION_JSON_VALUE)
    public Set<ResourceIdentifier> getRecordTypeTemplates() {
        return this.catalog.getIds(ComponentTypes.RECORD_TYPE);
    }

    @GetMapping(value = "/record_types/{template}", produces = MediaType.APPLICATION_JSON_VALUE)
    public RecordTypeDefinition getRecordTypeTemplate(@PathVariable("template") ResourceIdentifier templateId) {
        return ComponentTypes.RECORD_TYPE.getComponent(templateId, this.catalog)
                .orElseThrow(() -> ApiError.templateNotFound(ComponentTypes.RECORD_TYPE, templateId));
    }

    @PostMapping(value = "/record_types/{template}/apply", produces = MediaType.APPLICATION_JSON_VALUE)
    public RecordType applyRecordTypeTemplate(
            @PathVariable("template") ResourceIdentifier templateId,
            @RequestParam(value = "includeDependencies", required = false, defaultValue = "false") boolean includeDependencies
    ) {
        RecordTypeDefinition template = ComponentTypes.RECORD_TYPE.getComponent(templateId, this.catalog)
                .orElseThrow(() -> ApiError.templateNotFound(ComponentTypes.RECORD_TYPE, templateId));

        return ComponentTypes.RECORD_TYPE.register(this.repository, this.catalog, this.expressions, templateId, template, includeDependencies);
    }

    @GetMapping(value = "/lists", produces = MediaType.APPLICATION_JSON_VALUE)
    public Set<ResourceIdentifier> getTemplates() {
        return this.catalog.getIds(ComponentTypes.LIST);
    }

    @GetMapping(value = "/lists/{template}", produces = MediaType.APPLICATION_JSON_VALUE)
    public ListDefinition getTemplate(@PathVariable("template") ResourceIdentifier templateId) {
        return ComponentTypes.LIST.getComponent(templateId, this.catalog)
                .orElseThrow(() -> ApiError.templateNotFound(ComponentTypes.LIST, templateId));
    }

    @PostMapping(value = "/lists/{template}/apply", produces = MediaType.APPLICATION_JSON_VALUE)
    public ListType applyList(@PathVariable("template") ResourceIdentifier templateId) {
        ListDefinition listDef = ComponentTypes.LIST.getComponent(templateId, this.catalog)
                .orElseThrow(() -> ApiError.templateNotFound(ComponentTypes.LIST, templateId));

        ListType type = ComponentTypes.LIST.register(this.repository, this.catalog, this.expressions, templateId, listDef, false);

        listDef.defaultEntries.forEach((s, listItem) -> {
            ResourceIdentifier id = new ResourceIdentifier(templateId.source(), s);
            ComponentTypes.LIST_ELEMENT.register(this.repository, this.catalog, this.expressions, id, listItem, false);
        });

        return type;
    }

}
