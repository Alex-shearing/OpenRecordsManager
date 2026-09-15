package com.openrecordsmanager.plugin;

import com.openrecordsmanager.api.audit.AuditEntityType;
import com.openrecordsmanager.api.audit.AuditOperation;
import com.openrecordsmanager.api.builtin.BuiltinPlugin;
import com.openrecordsmanager.audit.AuditPropertyChange;
import com.openrecordsmanager.audit.AuditService;
import com.openrecordsmanager.audit.RequiresAuditComment;
import com.openrecordsmanager.database.DataRepository;
import com.openrecordsmanager.plugin.dto.PluginResponse;
import com.openrecordsmanager.plugin.dto.PluginTypeRequest;
import com.openrecordsmanager.plugin.dto.SimplePluginResponse;
import com.openrecordsmanager.plugin.dto.UpdatePluginRequest;
import com.openrecordsmanager.rest.errors.ResourceInUseException;
import com.openrecordsmanager.rest.errors.ResourceNotFoundException;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.io.IOException;
import java.io.InputStream;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.StandardCopyOption;
import java.util.ArrayList;
import java.util.List;
import java.util.Set;
import java.util.stream.Collectors;

@Service
public class PluginService {
    private final DataRepository repository;
    private final PluginManager pluginManager;
    private final PluginSyncService pluginSyncService;
    private final AuditService auditService;

    public PluginService(
            DataRepository repository,
            PluginManager pluginManager,
            PluginSyncService pluginSyncService,
            AuditService auditService
    ) {
        this.repository = repository;
        this.pluginManager = pluginManager;
        this.pluginSyncService = pluginSyncService;
        this.auditService = auditService;
    }

    @Transactional(readOnly = true)
    public Set<SimplePluginResponse> getAll(boolean includeDisabled) {
        Set<SimplePluginResponse> response = this.repository.pluginRepo.findAll().stream()
                .filter(plugin -> includeDisabled || plugin.isEnabled())
                .map(plugin -> SimplePluginResponse.of(plugin, this.pluginManager))
                .collect(Collectors.toSet());

        this.auditService.recordCollectionRead(AuditEntityType.PLUGIN, response.size());
        return response;
    }

    @Transactional(readOnly = true)
    public PluginResponse get(String name) {
        PersistedPlugin persistedPlugin = this.repository.pluginRepo.findById(name)
                .orElseThrow(() -> new ResourceNotFoundException("plugin", name));

        this.auditService.addReadEvent(AuditEntityType.PLUGIN, name);
        return PluginResponse.of(persistedPlugin, this.pluginManager);
    }

    @Transactional
    @RequiresAuditComment(operation = AuditOperation.CREATE, targetType = AuditEntityType.PLUGIN)
    public PluginResponse upload(InputStream archiveStream, PluginTypeRequest extension) throws IOException {
        Path tempDest = this.pluginManager.getDirectory()
                .resolve("upload")
                .resolve("upload-" + System.nanoTime() + "." + extension);

        try {
            // Copy to a temp file
            Files.createDirectories(tempDest.getParent());
            Files.copy(archiveStream, tempDest);

            // Parse metadata
            DiscoveredPlugin uploadedMeta = DiscoveredPlugin.read(tempDest, this.repository.pluginRepo);

            if (BuiltinPlugin.BUILTIN_PLUGIN_NAME.equals(uploadedMeta.id())) {
                throw new ResourceInUseException("the builtin plugin cannot be modified");
            }

            DiscoveredPlugin.PluginComparison comparison = uploadedMeta.compareLocalToPersisted();
            switch (comparison) {
                case PERSISTED_NEWER, EQUAL -> throw new ResourceInUseException(
                        "plugin already exists with same or newer version, uploaded version must be greater"
                );
                case SAME_VERSION_HASH_MISMATCH -> PluginManager.LOGGER.info(
                        "Uploading a new plugin file for {} with the same version but different hash",
                        uploadedMeta.id()
                );
            }

            Path finalDest = this.pluginManager.getDirectory().resolve(
                    uploadedMeta.id() + "-" + uploadedMeta.version() + "." + extension
            );
            Files.copy(tempDest, finalDest, StandardCopyOption.REPLACE_EXISTING);

            this.pluginSyncService.syncAndReload(true);

            PersistedPlugin plugin = this.repository.pluginRepo.findById(uploadedMeta.id())
                    .orElseThrow(() -> new ResourceNotFoundException("plugin", uploadedMeta.id()));

            if (uploadedMeta.persistedPlugin() == null) {
                this.auditService.addEvent(
                        AuditOperation.CREATE,
                        AuditEntityType.PLUGIN,
                        plugin.getName(),
                        null,
                        null,
                        PluginManager.auditMetadata(plugin)
                );
            } else {
                List<AuditPropertyChange> changes = List.of(
                        AuditPropertyChange.of(
                                "version",
                                uploadedMeta.persistedPlugin().getVersion(),
                                plugin.getVersion()
                        )
                );
                this.auditService.addEvent(
                        AuditOperation.UPDATE,
                        AuditEntityType.PLUGIN,
                        plugin.getName(),
                        changes,
                        null,
                        PluginManager.auditMetadata(plugin)
                );
            }

            return PluginResponse.of(plugin, this.pluginManager);
        } finally {
            Files.deleteIfExists(tempDest);
        }
    }

    @Transactional
    @RequiresAuditComment(operation = AuditOperation.UPDATE, targetType = AuditEntityType.PLUGIN)
    public PluginResponse update(String name, UpdatePluginRequest input) {
        if (BuiltinPlugin.BUILTIN_PLUGIN_NAME.equals(name)) {
            throw new ResourceInUseException("the builtin plugin cannot be modified");
        }

        PersistedPlugin plugin = this.repository.pluginRepo.findById(name)
                .orElseThrow(() -> new ResourceNotFoundException("plugin", name));

        List<AuditPropertyChange> changes = new ArrayList<>();
        if (input.enabled() != null && input.enabled() != plugin.isEnabled()) {
            boolean oldEnabled = plugin.isEnabled();
            plugin.setEnabled(input.enabled());
            changes.add(AuditPropertyChange.of("enabled", oldEnabled, input.enabled()));
        }

        this.repository.pluginRepo.saveAndFlush(plugin);

        if (!changes.isEmpty()) {
            this.auditService.addEvent(
                    AuditOperation.UPDATE,
                    AuditEntityType.PLUGIN,
                    name,
                    changes,
                    null,
                    null
            );
        }

        this.pluginSyncService.syncAndReload(true);

        return PluginResponse.of(plugin, this.pluginManager);
    }

    @Transactional
    @RequiresAuditComment(operation = AuditOperation.DELETE, targetType = AuditEntityType.PLUGIN)
    public void delete(String name) throws IOException {
        if (BuiltinPlugin.BUILTIN_PLUGIN_NAME.equals(name)) {
            throw new ResourceInUseException("the builtin plugin cannot be deleted");
        }

        PersistedPlugin plugin = this.repository.pluginRepo.findById(name)
                .orElseThrow(() -> new ResourceNotFoundException("plugin", name));

        this.pluginManager.deleteLocalPlugin(plugin);
        this.repository.pluginRepo.delete(plugin);

        this.auditService.addEvent(AuditOperation.DELETE, AuditEntityType.PLUGIN, name);

        this.pluginSyncService.syncAndReload(true);
    }
}
