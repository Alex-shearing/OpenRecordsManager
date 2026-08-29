package com.openrecordsmanager.filestore.middleware;

import com.openrecordsmanager.api.audit.AuditEntityType;
import com.openrecordsmanager.api.audit.AuditOperation;
import com.openrecordsmanager.api.filestore.FileStoreMiddlewareType;
import com.openrecordsmanager.api.types.ComponentTypes;
import com.openrecordsmanager.audit.AuditService;
import com.openrecordsmanager.audit.RequiresAuditComment;
import com.openrecordsmanager.database.DataRepository;
import com.openrecordsmanager.filestore.dto.MiddlewareResponse;
import com.openrecordsmanager.filestore.dto.MiddlewareTypeResponse;
import com.openrecordsmanager.filestore.dto.NewFileStoreMiddlewareRequest;
import com.openrecordsmanager.filestore.dto.SimpleMiddlewareResponse;
import com.openrecordsmanager.plugin.registry.ComponentCatalog;
import com.openrecordsmanager.rest.errors.ResourceInUseException;
import com.openrecordsmanager.rest.errors.ResourceNotFoundException;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.Map;
import java.util.Set;
import java.util.UUID;
import java.util.stream.Collectors;

@Service
public class MiddlewareService {
    private final DataRepository repository;
    private final ComponentCatalog catalog;
    private final AuditService auditService;

    public MiddlewareService(DataRepository repository, ComponentCatalog catalog, AuditService auditService) {
        this.repository = repository;
        this.catalog = catalog;
        this.auditService = auditService;
    }

    @Transactional(readOnly = true)
    public Set<SimpleMiddlewareResponse> getAll() {
        Set<SimpleMiddlewareResponse> results = this.repository.fileStoreMiddlewareRepo.findAll().stream()
                .map(middleware -> SimpleMiddlewareResponse.of(this.catalog, middleware))
                .collect(Collectors.toSet());
        this.auditService.recordCollectionRead(AuditEntityType.FILE_STORE_MIDDLEWARE, results.size());
        return results;
    }

    @Transactional(readOnly = true)
    public MiddlewareResponse get(UUID id) throws ResourceNotFoundException {
        Middleware middleware = this.repository.fileStoreMiddlewareRepo.findById(id)
                .orElseThrow(() -> new ResourceNotFoundException("stream store middleware", id.toString()));

        this.auditService.addReadEvent(AuditEntityType.FILE_STORE_MIDDLEWARE, id);
        return MiddlewareResponse.of(this.catalog, middleware);
    }

    @Transactional
    @RequiresAuditComment(operation = AuditOperation.CREATE, targetType = AuditEntityType.FILE_STORE_MIDDLEWARE)
    public SimpleMiddlewareResponse create(NewFileStoreMiddlewareRequest input) throws ResourceNotFoundException {
        FileStoreMiddlewareType<?> type = this.catalog.getRegistry(ComponentTypes.FILE_STORE_MIDDLEWARE).get(input.type())
                .orElseThrow(() -> new ResourceNotFoundException(ComponentTypes.FILE_STORE_MIDDLEWARE, input.type()));

        Middleware middleware = new Middleware(this.catalog, type, input.properties());

        this.repository.fileStoreMiddlewareRepo.saveAndFlush(middleware);

        this.auditService.addEvent(AuditOperation.CREATE, AuditEntityType.FILE_STORE_MIDDLEWARE, middleware.getId());

        return SimpleMiddlewareResponse.of(this.catalog, middleware);
    }

    @Transactional
    @RequiresAuditComment(operation = AuditOperation.UPDATE, targetType = AuditEntityType.FILE_STORE_MIDDLEWARE)
    public SimpleMiddlewareResponse update(UUID id, Map<String, ?> properties) throws ResourceNotFoundException {
        Middleware middleware = this.repository.fileStoreMiddlewareRepo.findById(id)
                .orElseThrow(() -> new ResourceNotFoundException("stream store middleware", id.toString()));

        middleware.setProperties(this.catalog, properties);

        this.repository.fileStoreMiddlewareRepo.saveAndFlush(middleware);

        this.auditService.addEvent(AuditOperation.UPDATE, AuditEntityType.FILE_STORE_MIDDLEWARE, id);

        return SimpleMiddlewareResponse.of(this.catalog, middleware);
    }

    @Transactional
    @RequiresAuditComment(operation = AuditOperation.DELETE, targetType = AuditEntityType.FILE_STORE_MIDDLEWARE)
    public void delete(UUID id) throws ResourceNotFoundException, ResourceInUseException {
        Middleware middleware = this.repository.fileStoreMiddlewareRepo.findById(id)
                .orElseThrow(() -> new ResourceNotFoundException("stream store middleware", id.toString()));

        if (this.repository.fileStoreRepo.existsByMiddlewares(middleware.getId())) {
            throw new ResourceInUseException("middleware is in use by one or more stream stores");
        }

        this.repository.fileStoreMiddlewareRepo.delete(middleware);

        this.auditService.addEvent(AuditOperation.DELETE, AuditEntityType.FILE_STORE_MIDDLEWARE, id);
    }

    @Transactional(readOnly = true)
    public MiddlewareTypeResponse[] getTypes() {
        return this.catalog.getRegistry(ComponentTypes.FILE_STORE_MIDDLEWARE).stream()
                .map(type -> MiddlewareTypeResponse.of(this.catalog, type))
                .toArray(MiddlewareTypeResponse[]::new);
    }
}
