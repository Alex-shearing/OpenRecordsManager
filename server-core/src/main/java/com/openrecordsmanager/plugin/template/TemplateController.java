package com.openrecordsmanager.plugin.template;

import com.openrecordsmanager.api.ResourceIdentifier;
import com.openrecordsmanager.api.errors.ResourceNotFoundException;
import com.openrecordsmanager.api.swagger.DefaultApiResponses;
import com.openrecordsmanager.api.swagger.NotFoundApiResponse;
import com.openrecordsmanager.api.types.ComponentType;
import com.openrecordsmanager.api.types.ComponentTypes;
import com.openrecordsmanager.plugin.registry.ComponentCatalog;
import com.openrecordsmanager.plugin.registry.TemplateComponentRegistry;
import io.swagger.v3.oas.annotations.Operation;
import org.springframework.http.MediaType;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.web.bind.annotation.*;

import java.util.Set;
import java.util.stream.Collectors;

@RestController
@RequestMapping("/api/templates")
@DefaultApiResponses
@PreAuthorize("isAuthenticated()")
public class TemplateController {

    private final ComponentCatalog catalog;
    private final TemplateService service;

    public TemplateController(ComponentCatalog catalog, TemplateService service) {
        this.catalog = catalog;
        this.service = service;
    }

    @GetMapping(produces = MediaType.APPLICATION_JSON_VALUE)
    @Operation(summary = "List templates types available")
    @Transactional(readOnly = true)
    public Set<String> getTemplatesForType() {
        return this.catalog.getTemplateTypes().stream().map(ComponentType::toString).collect(Collectors.toSet());
    }

    @GetMapping(value = "/{type}", produces = MediaType.APPLICATION_JSON_VALUE)
    @Operation(summary = "List templates available for type")
    @NotFoundApiResponse
    @Transactional(readOnly = true)
    public Set<ResourceIdentifier> getTemplatesForType(@PathVariable("type") String typeName) {
        ComponentType<?> type = ComponentTypes.fromName(typeName);
        if (type == null) {
            throw new ResourceNotFoundException("component type", typeName);
        }
        return this.catalog.getTemplateRegistry(type).getIds();
    }

    @GetMapping(value = "/{type}/{template}", produces = MediaType.APPLICATION_JSON_VALUE)
    @Operation(summary = "Get template details")
    @NotFoundApiResponse
    @Transactional(readOnly = true)
    public Object getTemplate(@PathVariable("type") String typeName, @PathVariable("template") ResourceIdentifier templateId) {
        ComponentType<?> type = ComponentTypes.fromName(typeName);
        if (type == null) {
            throw new ResourceNotFoundException("component type", typeName);
        }
        TemplateComponentRegistry<?, ?> registry = this.catalog.getTemplateRegistry(type);

        return registry.get(templateId)
                .orElseThrow(() -> new ResourceNotFoundException(type, templateId));
    }

    @PostMapping(value = "/{type}/{template}/register", produces = MediaType.APPLICATION_JSON_VALUE)
    @Operation(summary = "Register a template to the database")
    @NotFoundApiResponse
    public void registerTemplate(
            @PathVariable("type") String typeName,
            @PathVariable("template") ResourceIdentifier templateId,
            @RequestParam(value = "includeDependencies", required = false, defaultValue = "false") boolean includeDependencies
    ) {
        ComponentType<?> type = ComponentTypes.fromName(typeName);
        if (type == null) {
            throw new ResourceNotFoundException("component type", typeName);
        }

        this.service.registerTemplate(type, templateId, includeDependencies);
    }
}
