package com.openrecordsmanager.filestore.middleware;

import com.openrecordsmanager.api.errors.ResourceInUseException;
import com.openrecordsmanager.api.errors.ResourceNotFoundException;
import com.openrecordsmanager.api.filestore.FileStoreMiddlewareType;
import com.openrecordsmanager.api.types.ComponentTypes;
import com.openrecordsmanager.database.DataRepository;
import com.openrecordsmanager.filestore.dto.MiddlewareResponse;
import com.openrecordsmanager.filestore.dto.NewFileStoreMiddleware;
import com.openrecordsmanager.filestore.dto.SimpleFileStoreResponse;
import com.openrecordsmanager.filestore.dto.SimpleMiddlewareResponse;
import com.openrecordsmanager.plugin.registry.ComponentCatalog;
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

    public MiddlewareService(DataRepository repository, ComponentCatalog catalog) {
        this.repository = repository;
        this.catalog = catalog;
    }

    @Transactional(readOnly = true)
    public Set<SimpleMiddlewareResponse> getAll() {
        return this.repository.fileStoreMiddlewareRepo.findAll().stream()
                .map(middleware -> new SimpleMiddlewareResponse(middleware.id, middleware.type))
                .collect(Collectors.toSet());
    }

    @Transactional(readOnly = true)
    public MiddlewareResponse get(UUID id) throws ResourceNotFoundException {
        return this.repository.fileStoreMiddlewareRepo.findById(id)
                .map(middleware -> new MiddlewareResponse(
                        middleware.id,
                        middleware.type,
                        middleware.properties
                ))
                .orElseThrow(() -> new ResourceNotFoundException("stream store middleware", id.toString()));
    }

    @Transactional
    public SimpleMiddlewareResponse create(NewFileStoreMiddleware input) throws ResourceNotFoundException {
        FileStoreMiddlewareType<?> type = this.catalog.getRegistry(ComponentTypes.FILE_STORE_MIDDLEWARE).get(input.type())
                .orElseThrow(() -> new ResourceNotFoundException(ComponentTypes.FILE_STORE_MIDDLEWARE, input.type()));

        Middleware<?> middleware = new Middleware<>(this.catalog, type, input.properties());

        this.repository.fileStoreMiddlewareRepo.saveAndFlush(middleware);

        return new SimpleMiddlewareResponse(middleware.id, middleware.type);
    }

    @Transactional
    public SimpleFileStoreResponse update(UUID id, Map<String, ?> properties) throws ResourceNotFoundException {
        Middleware<?> middleware = this.repository.fileStoreMiddlewareRepo.findById(id)
                .orElseThrow(() -> new ResourceNotFoundException("stream store middleware", id.toString()));

        middleware.setProperties(properties);

        this.repository.fileStoreMiddlewareRepo.saveAndFlush(middleware);

        return new SimpleFileStoreResponse(middleware.id, middleware.type);
    }

    @Transactional
    public void delete(UUID id) throws ResourceNotFoundException, ResourceInUseException {
        Middleware<?> middleware = this.repository.fileStoreMiddlewareRepo.findById(id)
                .orElseThrow(() -> new ResourceNotFoundException("stream store middleware", id.toString()));

        if (this.repository.fileStoreRepo.existsByMiddlewares(middleware.id)) {
            throw new ResourceInUseException("middleware is in use by one or more stream stores");
        }

        this.repository.fileStoreMiddlewareRepo.delete(middleware);
    }
}
