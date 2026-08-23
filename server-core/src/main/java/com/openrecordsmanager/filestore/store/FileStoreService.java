package com.openrecordsmanager.filestore.store;

import com.google.common.hash.HashFunction;
import com.google.common.hash.Hashing;
import com.openrecordsmanager.api.filestore.FileStoreType;
import com.openrecordsmanager.api.types.ComponentTypes;
import com.openrecordsmanager.database.DataRepository;
import com.openrecordsmanager.filestore.dto.FileStoreResponse;
import com.openrecordsmanager.filestore.dto.FileStoreTypeResponse;
import com.openrecordsmanager.filestore.dto.NewFileStoreRequest;
import com.openrecordsmanager.filestore.dto.SimpleFileStoreResponse;
import com.openrecordsmanager.filestore.middleware.Middleware;
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
public class FileStoreService {
    public static final String CURRENT_HASH_ALGORITHM = "SHA-256";

    private final DataRepository repository;
    private final ComponentCatalog catalog;

    public FileStoreService(DataRepository repository, ComponentCatalog catalog) {
        this.repository = repository;
        this.catalog = catalog;
    }

    public static HashFunction getHashFunction(String algorithm) {
        return switch (algorithm) {
            case "SHA-256" -> Hashing.sha256();
            case "SHA-512" -> Hashing.sha512();
            default -> throw new IllegalStateException("Unknown hash function: " + algorithm);
        };
    }

    @Transactional(readOnly = true)
    public Set<SimpleFileStoreResponse> getAll() {
        return this.repository.fileStoreRepo.findAll().stream()
                .map(fileStore -> SimpleFileStoreResponse.of(this.catalog, fileStore))
                .collect(Collectors.toSet());
    }

    @Transactional(readOnly = true)
    public FileStoreResponse get(UUID id) throws ResourceNotFoundException {
        return this.repository.fileStoreRepo.findById(id)
                .map(fileStore -> FileStoreResponse.of(this.catalog, fileStore))
                .orElseThrow(() -> new ResourceNotFoundException("file store", id.toString()));
    }

    @Transactional
    public SimpleFileStoreResponse create(NewFileStoreRequest input) throws ResourceNotFoundException {
        FileStoreType<?> type = this.catalog.getRegistry(ComponentTypes.FILE_STORE).get(input.type())
                .orElseThrow(() -> new ResourceNotFoundException(ComponentTypes.FILE_STORE, input.type()));

        FileStore store = new FileStore(this.catalog, type, input.properties());

        for (UUID middleware : input.middlewares()) {
            Middleware mw = this.repository.fileStoreMiddlewareRepo.findById(middleware)
                    .orElseThrow(() -> new ResourceNotFoundException("file store middleware", middleware));

            store.addMiddleware(mw);
        }

        this.repository.fileStoreRepo.saveAndFlush(store);

        return SimpleFileStoreResponse.of(this.catalog, store);
    }

    @Transactional
    public SimpleFileStoreResponse update(UUID id, Map<String, ?> properties) throws ResourceNotFoundException {
        FileStore store = this.repository.fileStoreRepo.findById(id)
                .orElseThrow(() -> new ResourceNotFoundException("file store", id));
        store.setProperties(this.catalog, properties);

        this.repository.fileStoreRepo.saveAndFlush(store);

        return SimpleFileStoreResponse.of(this.catalog, store);
    }

    @Transactional
    public void delete(UUID id) throws ResourceNotFoundException, ResourceInUseException {
        FileStore store = this.repository.fileStoreRepo.findById(id)
                .orElseThrow(() -> new ResourceNotFoundException("file store", id));

        if (!store.getFiles().isEmpty()) {
            throw new ResourceInUseException("stream store has contents and cannot be deleted");
        }

        this.repository.fileStoreRepo.delete(store);
    }

    @Transactional(readOnly = true)
    public FileStoreTypeResponse[] getTypes() {
        return this.catalog.getRegistry(ComponentTypes.FILE_STORE).stream()
                .map(type -> FileStoreTypeResponse.of(this.catalog, type))
                .toArray(FileStoreTypeResponse[]::new);
    }

}
