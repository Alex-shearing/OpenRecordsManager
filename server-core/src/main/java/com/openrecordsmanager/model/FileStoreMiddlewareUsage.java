package com.openrecordsmanager.model;

import jakarta.persistence.*;

@Entity
@Table(name = "file_store_middlewares_usage")
public class FileStoreMiddlewareUsage {

    @EmbeddedId
    public FileStoreMiddlewareUsageId id;

    @ManyToOne()
    @MapsId("fileStoreId")
    @JoinColumn(name = "file_store_id")
    public FileStore<?> fileStore;

    @ManyToOne()
    @MapsId("middlewareId")
    @JoinColumn(name = "middleware_id")
    public FileStoreMiddleware<?> middleware;

    @Column(name = "application_order")
    public int applicationOrder;

    public FileStoreMiddlewareUsage(FileStore<?> fileStore, FileStoreMiddleware<?> middleware, int order) {
        this.id = new FileStoreMiddlewareUsageId(fileStore.id, middleware.id);
        this.fileStore = fileStore;
        this.middleware = middleware;
        this.applicationOrder = order;
    }

    @Deprecated
    protected FileStoreMiddlewareUsage() {
    }
}
