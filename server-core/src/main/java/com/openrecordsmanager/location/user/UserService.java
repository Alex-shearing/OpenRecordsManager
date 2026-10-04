package com.openrecordsmanager.location.user;

import com.openrecordsmanager.api.audit.AuditEntityType;
import com.openrecordsmanager.api.audit.AuditOperation;
import com.openrecordsmanager.api.errors.ApiException;
import com.openrecordsmanager.audit.AuditPropertyChange;
import com.openrecordsmanager.audit.AuditService;
import com.openrecordsmanager.auth.entity.AuthProvider;
import com.openrecordsmanager.database.DataRepository;
import com.openrecordsmanager.location.dto.LocationResponse;
import com.openrecordsmanager.location.dto.NewLocationRequest;
import com.openrecordsmanager.location.dto.UpdateLocationRequest;
import com.openrecordsmanager.location.type.LocationType;
import com.openrecordsmanager.property.ObjectPropertyApplier;
import com.openrecordsmanager.rest.exception.ResourceAlreadyExistsException;
import com.openrecordsmanager.rest.exception.ResourceInUseException;
import com.openrecordsmanager.rest.exception.ResourceNotFoundException;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.ArrayList;
import java.util.List;
import java.util.UUID;

@Service
public class UserService {

    private final DataRepository repository;
    private final AuditService auditService;
    private final ObjectPropertyApplier propertyApplier;

    public UserService(
            DataRepository repository,
            AuditService auditService,
            ObjectPropertyApplier propertyApplier
    ) {
        this.repository = repository;
        this.auditService = auditService;
        this.propertyApplier = propertyApplier;
    }

    @Transactional
    public LocationResponse create(LocationType type, NewLocationRequest input) {
        String name = input.name();
        if (name == null || name.isBlank()) {
            throw ApiException.validationFailed("name", "required");
        }

        if (this.repository.locationRepo.findByName(name).isPresent()) {
            throw new ResourceAlreadyExistsException("location", name);
        }

        AuthProvider authProvider = null;
        if (input.authProvider() != null) {
            authProvider = this.repository.authProviderRepo.findById(input.authProvider())
                    .orElseThrow(() -> new ResourceNotFoundException("authentication provider", input.authProvider()));
        }

        List<AuditPropertyChange> changes = new ArrayList<>();
        changes.add(AuditPropertyChange.newProperty("type", input.type()));
        changes.add(AuditPropertyChange.newProperty("name", name));
        changes.add(AuditPropertyChange.newProperty("authProvider", input.authProvider()));

        User user = new User(name, authProvider, type);
        this.propertyApplier.applyOnCreate(user, input.properties(), true, changes);

        this.repository.userRepo.saveAndFlush(user);

        this.auditService.addEvent(
                AuditOperation.CREATE,
                AuditEntityType.LOCATION,
                user.getId().toString(),
                changes,
                null,
                null
        );

        return LocationResponse.of(user);
    }

    @Transactional
    public LocationResponse update(User actor, User user, UpdateLocationRequest input) {
        List<AuditPropertyChange> changes = new ArrayList<>();

        if (input.name() != null && !input.name().equals(user.getName())) {
            if (this.repository.locationRepo.findByName(input.name()).isPresent()) {
                throw new ResourceAlreadyExistsException("location", input.name());
            }

            String oldName = user.getName();
            user.setName(input.name());
            changes.add(AuditPropertyChange.of("name", oldName, input.name()));
        }

        if (input.authProvider() != null && (user.getAuthProvider() == null || !input.authProvider().equals(user.getAuthProvider().getId()))) {
            AuthProvider authProvider = this.repository.authProviderRepo.findById(input.authProvider())
                    .orElseThrow(() -> new ResourceNotFoundException("authentication provider", input.authProvider()));

            UUID oldProviderId = user.getAuthProvider() != null ? user.getAuthProvider().getId() : null;
            user.setAuthProvider(authProvider);
            changes.add(AuditPropertyChange.of("authProvider", oldProviderId, authProvider.getId()));
        }

        if (input.enabled() != null && input.enabled() != user.isEnabled()) {
            if (!input.enabled() && actor.getId().equals(user.getId())) {
                throw new ResourceInUseException("current user");
            }

            boolean oldEnabled = user.isEnabled();
            user.setEnabled(input.enabled());
            changes.add(AuditPropertyChange.of("enabled", oldEnabled, input.enabled()));

            if (!input.enabled()) {
                user.bumpSessionEpoch();
            }
        }

        if (input.properties() != null) {
            this.propertyApplier.applyOnUpdate(user, input.properties(), true, changes);
        }

        this.repository.userRepo.saveAndFlush(user);

        this.auditService.addEvent(
                AuditOperation.UPDATE,
                AuditEntityType.LOCATION,
                user.getId().toString(),
                changes.isEmpty() ? null : changes,
                null,
                null
        );

        return LocationResponse.of(user);
    }
}
