package com.openrecordsmanager.controllers;

import com.openrecordsmanager.api.Component;
import com.openrecordsmanager.controllers.repsonse.InternalServerErrorApiResponse;
import com.openrecordsmanager.controllers.repsonse.NotFoundApiResponse;
import com.openrecordsmanager.controllers.repsonse.errors.ApiError;
import com.openrecordsmanager.model.repositories.DataRepository;
import com.openrecordsmanager.resources.ComponentCatalog;
import com.openrecordsmanager.resources.ExpressionsService;
import com.openrecordsmanager.resources.ResourceIdentifier;
import com.openrecordsmanager.resources.types.ComponentType;
import com.openrecordsmanager.resources.types.ComponentTypes;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.responses.ApiResponse;
import org.springframework.http.MediaType;
import org.springframework.web.bind.annotation.*;

import java.util.Arrays;
import java.util.Set;
import java.util.stream.Collectors;

@RestController
@RequestMapping("/templates")
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
    public Set<String> getTemplatesForType() {
        return Arrays.stream(ComponentTypes.VALUES)
                .map(componentType -> componentType.name)
                .collect(Collectors.toSet());
    }

    @GetMapping(value = "/{type}", produces = MediaType.APPLICATION_JSON_VALUE)
    @Operation(summary = "List templates available for type")
    @NotFoundApiResponse
    public Set<ResourceIdentifier> getTemplatesForType(@PathVariable("type") String typeName) {
        ComponentType<?, ?> type = ComponentTypes.fromName(typeName);
        if (type == null) {
            throw ApiError.notFound("template type", typeName);
        }

        return this.catalog.getIds(type);
    }

    @GetMapping(value = "/{type}/{template}", produces = MediaType.APPLICATION_JSON_VALUE)
    @Operation(summary = "Get template details")
    @NotFoundApiResponse
    @SuppressWarnings("unchecked")
    public <T> T getTemplatesForType(@PathVariable("type") String typeName, @PathVariable("template") ResourceIdentifier templateId) {
        ComponentType<?, T> type = (ComponentType<?, T>) ComponentTypes.fromName(typeName);
        if (type == null) {
            throw ApiError.notFound("template type", typeName);
        }

        return type.getRegistered(templateId, this.repository)
                .orElseThrow(() -> ApiError.notFound(type, templateId));
    }

    @GetMapping(value = "/{type}/{template}/register", produces = MediaType.APPLICATION_JSON_VALUE)
    @Operation(summary = "Register a template to the database")
    @NotFoundApiResponse
    @SuppressWarnings("unchecked")
    public <T extends Component> void registerTemplate(
            @PathVariable("type") String typeName,
            @PathVariable("template") ResourceIdentifier templateId,
            @RequestParam(value = "includeDependencies", required = false, defaultValue = "false") boolean includeDependencies
    ) {
        ComponentType<T, ?> type = (ComponentType<T, ?>) ComponentTypes.fromName(typeName);
        if (type == null) {
            throw ApiError.notFound("template type", typeName);
        }

        T template = type.getComponent(templateId, this.catalog)
                .orElseThrow(() -> ApiError.templateNotFound(ComponentTypes.RECORD_TYPE, templateId));

        type.register(this.repository, this.catalog, this.expressions, templateId, template, includeDependencies);
    }
}
