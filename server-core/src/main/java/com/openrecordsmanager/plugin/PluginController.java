package com.openrecordsmanager.plugin;

import com.openrecordsmanager.plugin.dto.PluginResponse;
import com.openrecordsmanager.plugin.dto.PluginTypeRequest;
import com.openrecordsmanager.plugin.dto.SimplePluginResponse;
import com.openrecordsmanager.plugin.dto.UpdatePluginRequest;
import com.openrecordsmanager.rest.swagger.ConflictApiResponse;
import com.openrecordsmanager.rest.swagger.DefaultApiResponses;
import com.openrecordsmanager.rest.swagger.NotFoundApiResponse;
import io.swagger.v3.oas.annotations.Operation;
import org.springframework.http.MediaType;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.multipart.MultipartFile;

import java.io.IOException;
import java.util.Set;

@RestController
@RequestMapping("/api/plugins")
@DefaultApiResponses
@PreAuthorize("isAuthenticated()")
public class PluginController {

    private final PluginService service;

    public PluginController(PluginService service) {
        this.service = service;
    }

    @GetMapping(produces = MediaType.APPLICATION_JSON_VALUE)
    @Operation(summary = "List plugins")
    public Set<SimplePluginResponse> listPlugins(@RequestParam(defaultValue = "false") boolean includeDisabled) {
        return this.service.getAll(includeDisabled);
    }

    @GetMapping(value = "/{id}", produces = MediaType.APPLICATION_JSON_VALUE)
    @Operation(summary = "Get plugin details")
    @NotFoundApiResponse
    public PluginResponse getPlugin(@PathVariable("id") String id) {
        return this.service.get(id);
    }

    @PostMapping(consumes = MediaType.MULTIPART_FORM_DATA_VALUE, produces = MediaType.APPLICATION_JSON_VALUE)
    @Operation(summary = "Upload a plugin JAR or template ZIP")
    @ConflictApiResponse
    public PluginResponse uploadPlugin(
            @RequestPart("file") MultipartFile file,
            @RequestParam("type") PluginTypeRequest type
    ) throws IOException {
        return this.service.upload(file.getInputStream(), type);
    }

    @PutMapping(value = "/{id}", consumes = MediaType.APPLICATION_JSON_VALUE, produces = MediaType.APPLICATION_JSON_VALUE)
    @Operation(summary = "Update a plugin")
    @NotFoundApiResponse
    public PluginResponse updatePlugin(
            @PathVariable("id") String id,
            @RequestBody UpdatePluginRequest input
    ) {
        return this.service.update(id, input);
    }

    @DeleteMapping(value = "/{id}", produces = MediaType.APPLICATION_JSON_VALUE)
    @Operation(summary = "Delete a plugin")
    @NotFoundApiResponse
    public void deletePlugin(@PathVariable("id") String id) throws IOException {
        this.service.delete(id);
    }
}
