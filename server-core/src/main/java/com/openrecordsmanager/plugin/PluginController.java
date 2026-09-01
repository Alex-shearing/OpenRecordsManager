package com.openrecordsmanager.plugin;

import com.openrecordsmanager.plugin.dto.PluginResponse;
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

    @GetMapping(value = "/{name}", produces = MediaType.APPLICATION_JSON_VALUE)
    @Operation(summary = "Get plugin details")
    @NotFoundApiResponse
    public PluginResponse getPlugin(@PathVariable("name") String name) {
        return this.service.get(name);
    }

    @PostMapping(consumes = MediaType.MULTIPART_FORM_DATA_VALUE, produces = MediaType.APPLICATION_JSON_VALUE)
    @Operation(summary = "Upload a plugin JAR")
    @ConflictApiResponse
    public PluginResponse uploadPlugin(@RequestPart("jar") MultipartFile jar) throws IOException {
        return this.service.upload(jar.getInputStream());
    }

    @PutMapping(value = "/{name}", consumes = MediaType.APPLICATION_JSON_VALUE, produces = MediaType.APPLICATION_JSON_VALUE)
    @Operation(summary = "Update a plugin")
    @NotFoundApiResponse
    public PluginResponse updatePlugin(
            @PathVariable("name") String name,
            @RequestBody UpdatePluginRequest input
    ) {
        return this.service.update(name, input);
    }

    @DeleteMapping(value = "/{name}", produces = MediaType.APPLICATION_JSON_VALUE)
    @Operation(summary = "Delete a plugin")
    @NotFoundApiResponse
    public void deletePlugin(@PathVariable("name") String name) throws IOException {
        this.service.delete(name);
    }
}
