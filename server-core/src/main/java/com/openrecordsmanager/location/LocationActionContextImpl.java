package com.openrecordsmanager.location;

import com.openrecordsmanager.api.audit.AuditEmitter;
import com.openrecordsmanager.api.audit.AuditEntityType;
import com.openrecordsmanager.api.config.ConfigStore;
import com.openrecordsmanager.api.location.LocationActionContext;
import com.openrecordsmanager.api.location.LocationKind;
import com.openrecordsmanager.api.template.property.ObjectPropertyTemplate;
import com.openrecordsmanager.api.types.ComponentTypes;
import com.openrecordsmanager.audit.AuditEmitterImpl;
import com.openrecordsmanager.audit.AuditService;
import com.openrecordsmanager.database.DataRepository;
import com.openrecordsmanager.location.user.User;
import com.openrecordsmanager.plugin.registry.ComponentCatalog;
import com.openrecordsmanager.property.ObjectProperty;
import com.openrecordsmanager.rest.exception.ResourceNotFoundException;
import org.jspecify.annotations.Nullable;

import java.util.Optional;
import java.util.UUID;

class LocationActionContextImpl implements LocationActionContext {
    private final DataRepository repository;
    private final ComponentCatalog catalog;
    private final ConfigStore config;
    private final AuditService auditService;
    private final User actor;
    private final Location target;

    LocationActionContextImpl(
            DataRepository repository,
            ComponentCatalog catalog,
            ConfigStore config,
            AuditService auditService,
            User actor,
            Location target
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
        return this.actor.getName();
    }

    @Override
    public UUID getTargetLocationId() {
        return this.target.getId();
    }

    @Override
    public LocationKind getTargetKind() {
        return this.target.getKind();
    }

    @Override
    public Optional<String> getTargetUsername() {
        if (this.target instanceof User user) {
            return Optional.of(user.getName());
        }
        return Optional.empty();
    }

    @Override
    public String getTargetName() {
        return this.target.getDisplayName();
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
    public <T> void setTargetProperty(ObjectPropertyTemplate<T> property, @Nullable T value) {
        @SuppressWarnings("unchecked")
        ObjectProperty<T> prop = (ObjectProperty<T>) this.catalog.getTemplateRegistry(ComponentCatalog.OBJECT_PROPERTY_MAPPER)
                .getRegistered(property, this.repository)
                .orElseThrow(() -> new ResourceNotFoundException(ComponentTypes.OBJECT_PROPERTY, property.toString()));

        T oldValue = this.target.getProperty(prop);
        this.target.setProperty(prop, value);

        this.repository.locationRepo.saveAndFlush(this.target);

        if (oldValue != value) {
            this.getAudit().addPropertyChangeEvent(prop.getId().toString(), oldValue, value);
        }
    }

    @Override
    public AuditEmitter getAudit() {
        return new AuditEmitterImpl(this.auditService, AuditEntityType.LOCATION, this.target.getId().toString());
    }
}
