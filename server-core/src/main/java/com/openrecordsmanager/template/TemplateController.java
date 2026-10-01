package com.openrecordsmanager.template;

import com.openrecordsmanager.api.ResourceIdentifier;
import com.openrecordsmanager.api.template.TemplateComponent;
import com.openrecordsmanager.rest.dto.ComponentReferenceDto;
import com.openrecordsmanager.rest.swagger.DefaultApiResponses;
import com.openrecordsmanager.rest.swagger.NotFoundApiResponse;
import io.swagger.v3.oas.annotations.Operation;
import org.jspecify.annotations.Nullable;
import org.springframework.http.MediaType;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.*;

import java.util.Set;

@RestController
@RequestMapping("/api/templates")
@DefaultApiResponses
@PreAuthorize("isAuthenticated()")
public class TemplateController {

    private final TemplateService service;

    public TemplateController(TemplateService service) {
        this.service = service;
    }

    @GetMapping(produces = MediaType.APPLICATION_JSON_VALUE)
    @Operation(summary = "List templates available")
    @NotFoundApiResponse
    public Set<ComponentReferenceDto> listTemplates(
            @RequestParam(value = "type", required = false) @Nullable String type
    ) {
        return this.service.listTemplates(type);
    }

    @GetMapping(value = "/{type}/{template}", produces = MediaType.APPLICATION_JSON_VALUE)
    @Operation(summary = "Get template details")
    @NotFoundApiResponse
    public TemplateComponent getTemplate(@PathVariable("type") String typeName, @PathVariable("template") ResourceIdentifier templateId) {
        return this.service.getTemplate(typeName, templateId);
    }

    @PostMapping(value = "/{type}/{template}/register", produces = MediaType.APPLICATION_JSON_VALUE)
    @Operation(summary = "Register a template to the database")
    @NotFoundApiResponse
    public void registerTemplate(
            @PathVariable("type") String typeName,
            @PathVariable("template") ResourceIdentifier templateId,
            @RequestParam(value = "includeDependencies", required = false, defaultValue = "false") boolean includeDependencies
    ) {
        this.service.registerTemplate(typeName, templateId, includeDependencies);
    }
}
