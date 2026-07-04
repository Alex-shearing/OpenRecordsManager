package com.openrecordsmanager.model;

import jakarta.persistence.Column;
import jakarta.persistence.Embeddable;

import java.io.Serializable;
import java.util.Objects;
import java.util.UUID;

@Embeddable
public class FileStoreMiddlewareUsageId implements Serializable {

    @Column(name = "file_store_id")
    private UUID fileStoreId;

    @Column(name = "middleware_id")
    private UUID middlewareId;

    public FileStoreMiddlewareUsageId(UUID fileStoreId, UUID middlewareId) {
        this.fileStoreId = fileStoreId;
        this.middlewareId = middlewareId;
    }

    @Deprecated
    protected FileStoreMiddlewareUsageId() {
    }

    @Override
    public boolean equals(Object o) {
        if (!(o instanceof FileStoreMiddlewareUsageId that)) return false;
        return Objects.equals(fileStoreId, that.fileStoreId) &&
                Objects.equals(middlewareId, that.middlewareId);
    }

    @Override
    public int hashCode() {
        return Objects.hash(fileStoreId, middlewareId);
    }
}
