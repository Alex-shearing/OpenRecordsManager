package com.openrecordsmanager.controllers;

import com.openrecordsmanager.api.Component;
import com.openrecordsmanager.api.ResourceIdentifier;
import com.openrecordsmanager.api.types.ComponentType;
import com.openrecordsmanager.api.types.ComponentTypes;
import com.openrecordsmanager.controllers.repsonse.InternalServerErrorApiResponse;
import com.openrecordsmanager.controllers.repsonse.NotFoundApiResponse;
import com.openrecordsmanager.controllers.repsonse.errors.ApiError;
import com.openrecordsmanager.model.repositories.DataRepository;
import com.openrecordsmanager.resources.ComponentCatalog;
import com.openrecordsmanager.resources.ExpressionsService;
import com.openrecordsmanager.resources.types.ComponentBinder;
import com.openrecordsmanager.resources.types.ComponentBinderRegistry;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.responses.ApiResponse;
import org.springframework.http.MediaType;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.web.bind.annotation.*;

import java.util.Arrays;
import java.util.Set;
import java.util.stream.Collectors;

@RestController
@RequestMapping("/api/templates")
@InternalServerErrorApiResponse
@ApiResponse(responseCode = "200")
public class TemplateController {

    private final ComponentCatalog catalog;
    private final ExpressionsService expressions;
    private final DataRepository repository;

    public TemplateController(ComponentCatalog catalog, ExpressionsService expressions, DataRepository repository) {
        this.catalog = catalog;
        this.expressions = expressions;
        this.repository = repository;
    }

    @GetMapping(produces = MediaType.APPLICATION_JSON_VALUE)
    @Operation(summary = "List templates types available")
    @Transactional(readOnly = true)
    public Set<String> getTemplatesForType() {
        return Arrays.stream(ComponentTypes.VALUES)
                .map(componentType -> componentType.name)
                .collect(Collectors.toSet());
    }

    @GetMapping(value = "/{type}", produces = MediaType.APPLICATION_JSON_VALUE)
    @Operation(summary = "List templates available for type")
    @NotFoundApiResponse
    @Transactional(readOnly = true)
    public Set<ResourceIdentifier> getTemplatesForType(@PathVariable("type") String typeName) {
        ComponentType<?> type = ComponentTypes.fromName(typeName);
        if (type == null) {
            throw ApiError.notFound("template type", typeName);
        }

        return this.catalog.getIds(type);
    }

    @GetMapping(value = "/{type}/{template}", produces = MediaType.APPLICATION_JSON_VALUE)
    @Operation(summary = "Get template details")
    @NotFoundApiResponse
    @Transactional(readOnly = true)
    public Object getTemplatesForType(@PathVariable("type") String typeName, @PathVariable("template") ResourceIdentifier templateId) {
        ComponentType<? extends Component> type = ComponentTypes.fromName(typeName);
        if (type == null) {
            throw ApiError.notFound("template type", typeName);
        }
        ComponentBinder<?, ?> binding = ComponentBinderRegistry.get(type);

        return binding.getRegistered(templateId, this.repository)
                .orElseThrow(() -> ApiError.notFound(type, templateId));
    }

    @PostMapping(value = "/{type}/{template}/register", produces = MediaType.APPLICATION_JSON_VALUE)
    @Operation(summary = "Register a template to the database")
    @NotFoundApiResponse
    public void registerTemplate(
            @PathVariable("type") String typeName,
            @PathVariable("template") ResourceIdentifier templateId,
            @RequestParam(value = "includeDependencies", required = false, defaultValue = "false") boolean includeDependencies
    ) {
        ComponentType<Component> type = ComponentTypes.fromName(typeName);
        if (type == null) {
            throw ApiError.notFound("template type", typeName);
        }
        ComponentBinder<Component, ?> binding = ComponentBinderRegistry.get(type);


        Component template = this.catalog.getComponent(type, templateId)
                .orElseThrow(() -> ApiError.templateNotFound(ComponentTypes.RECORD_TYPE, templateId));

        binding.register(this.repository, this.catalog, this.expressions, templateId, template, includeDependencies);
    }
}
