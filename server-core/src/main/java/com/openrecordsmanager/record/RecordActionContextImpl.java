package com.openrecordsmanager.record;

import com.openrecordsmanager.api.audit.AuditEmitter;
import com.openrecordsmanager.api.audit.AuditEntityType;
import com.openrecordsmanager.api.config.ConfigStore;
import com.openrecordsmanager.api.record.RecordActionContext;
import com.openrecordsmanager.api.template.property.ObjectPropertyTemplate;
import com.openrecordsmanager.api.types.ComponentTypes;
import com.openrecordsmanager.audit.AuditEmitterImpl;
import com.openrecordsmanager.audit.AuditService;
import com.openrecordsmanager.database.DataRepository;
import com.openrecordsmanager.plugin.registry.ComponentCatalog;
import com.openrecordsmanager.property.ObjectProperty;
import com.openrecordsmanager.rest.errors.ResourceNotFoundException;
import com.openrecordsmanager.user.User;
import org.jspecify.annotations.Nullable;

import java.util.Optional;
import java.util.UUID;

class RecordActionContextImpl implements RecordActionContext {
    private final DataRepository repository;
    private final ComponentCatalog catalog;
    private final ConfigStore config;
    private final AuditService auditService;
    private final User actor;
    private final Record target;

    RecordActionContextImpl(
            DataRepository repository,
            ComponentCatalog catalog,
            ConfigStore config,
            AuditService auditService,
            User actor,
            Record target
    ) {
        this.repository = repository;
        this.catalog = catalog;
        this.config = config;
        this.auditService = auditService;
        this.actor = actor;
        this.target = target;
    }

    @Override
    public UUID getActorId() {
        return this.actor.getId();
    }

    @Override
    public String getActorUsername() {
        return this.actor.getUsername();
    }

    @Override
    public UUID getTargetRecordId() {
        return this.target.getId();
    }

    @Override
    public ConfigStore getConfig() {
        return this.config;
    }

    @Override
    public <T> boolean isPropertyRegistered(ObjectPropertyTemplate<T> property) {
        return this.catalog.getTemplateRegistry(ComponentCatalog.OBJECT_PROPERTY_MAPPER)
                .getRegistered(property, this.repository)
                .isPresent();
    }

    @Override
    @SuppressWarnings("unchecked")
    public <T> Optional<T> getTargetProperty(ObjectPropertyTemplate<T> property) {
        Optional<ObjectProperty<?>> prop = this.catalog.getTemplateRegistry(ComponentCatalog.OBJECT_PROPERTY_MAPPER)
                .getRegistered(property, this.repository);

        if (prop.isEmpty()) {
            return Optional.empty();
        }

        ObjectProperty<T> typedProp = (ObjectProperty<T>) prop.get();
        return Optional.ofNullable(this.target.getProperty(typedProp));
    }

    @Override
    @SuppressWarnings("unchecked")
    public <T> void setTargetProperty(ObjectPropertyTemplate<T> property, @Nullable T value) {
        ObjectProperty<?> prop = this.catalog.getTemplateRegistry(ComponentCatalog.OBJECT_PROPERTY_MAPPER)
                .getRegistered(property, this.repository)
                .orElseThrow(() -> new ResourceNotFoundException(ComponentTypes.OBJECT_PROPERTY, property.getClass()));

        ObjectProperty<T> typedProp = (ObjectProperty<T>) prop;
        T oldValue = this.target.getProperty(typedProp);
        this.target.setProperty(typedProp, value);
        this.repository.recordRepo.saveAndFlush(this.target);
        getAudit().addPropertyChangeEvent(typedProp.getId().toString(), oldValue, value);
    }

    @Override
    public AuditEmitter getAudit() {
        return new AuditEmitterImpl(this.auditService, AuditEntityType.RECORD, this.target.getId().toString());
    }
}
