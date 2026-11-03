package com.openrecordsmanager.action;

import com.openrecordsmanager.api.ComponentReference;
import com.openrecordsmanager.api.action.ActionContext;
import com.openrecordsmanager.api.audit.AuditEmitter;
import com.openrecordsmanager.api.audit.AuditEntityType;
import com.openrecordsmanager.api.config.ConfigStore;
import com.openrecordsmanager.api.property.ManagedObjectKind;
import com.openrecordsmanager.api.template.property.ObjectPropertyTemplate;
import com.openrecordsmanager.api.types.ComponentTypes;
import com.openrecordsmanager.audit.AuditEmitterImpl;
import com.openrecordsmanager.audit.AuditService;
import com.openrecordsmanager.database.DataRepository;
import com.openrecordsmanager.location.user.User;
import com.openrecordsmanager.plugin.registry.ComponentCatalog;
import com.openrecordsmanager.property.ManagedObject;
import com.openrecordsmanager.property.ObjectProperty;
import com.openrecordsmanager.rest.exception.ResourceNotFoundException;
import org.jspecify.annotations.Nullable;

import java.util.Optional;
import java.util.UUID;
import java.util.function.Consumer;

public class ActionContextImpl implements ActionContext {
    private final DataRepository repository;
    private final ComponentCatalog catalog;
    private final ConfigStore config;
    private final AuditService auditService;
    private final User actor;
    private final ManagedObject<?> target;
    private final AuditEntityType auditEntityType;
    private final Consumer<ManagedObject<?>> persist;

    public ActionContextImpl(
            DataRepository repository,
            ComponentCatalog catalog,
            ConfigStore config,
            AuditService auditService,
            User actor,
            ManagedObject<?> target,
            AuditEntityType auditEntityType,
            Consumer<ManagedObject<?>> persist
    ) {
        this.repository = repository;
        this.catalog = catalog;
        this.config = config;
        this.auditService = auditService;
        this.actor = actor;
        this.target = target;
        this.auditEntityType = auditEntityType;
        this.persist = persist;
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
    public UUID getTargetId() {
        return this.target.getId();
    }

    @Override
    public ManagedObjectKind getTargetKind() {
        return this.target.getKind();
    }

    @Override
    public ConfigStore getConfig() {
        return this.config;
    }

    @Override
    public <T> boolean isPropertyRegistered(ComponentReference<ObjectPropertyTemplate<T>> property) {
        return this.catalog.getTemplateRegistry(ComponentCatalog.OBJECT_PROPERTY_MAPPER)
                .getRegistered(property.id(), this.repository)
                .isPresent();
    }

    @Override
    @SuppressWarnings("unchecked")
    public <T> Optional<T> getTargetProperty(ComponentReference<ObjectPropertyTemplate<T>> property) {
        Optional<ObjectProperty<?>> prop = this.catalog.getTemplateRegistry(ComponentCatalog.OBJECT_PROPERTY_MAPPER)
                .getRegistered(property.id(), this.repository);

        if (prop.isEmpty()) {
            return Optional.empty();
        }

        ObjectProperty<T> typedProp = (ObjectProperty<T>) prop.get();
        return Optional.ofNullable(this.target.getProperty(typedProp));
    }

    @Override
    public <T> void setTargetProperty(ComponentReference<ObjectPropertyTemplate<T>> property, @Nullable T value) {
        @SuppressWarnings("unchecked")
        ObjectProperty<T> prop = (ObjectProperty<T>) this.catalog.getTemplateRegistry(ComponentCatalog.OBJECT_PROPERTY_MAPPER)
                .getRegistered(property.id(), this.repository)
                .orElseThrow(() -> new ResourceNotFoundException(ComponentTypes.OBJECT_PROPERTY, property.toString()));

        T oldValue = this.target.getProperty(prop);
        this.target.setProperty(prop, value);

        this.persist.accept(this.target);

        if (oldValue != value) {
            this.getAudit().addPropertyChangeEvent(prop.getId().toString(), oldValue, value);
        }
    }

    @Override
    public AuditEmitter getAudit() {
        return new AuditEmitterImpl(this.auditService, this.auditEntityType, this.target.getId().toString());
    }
}
