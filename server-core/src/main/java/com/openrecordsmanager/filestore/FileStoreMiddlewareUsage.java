package com.openrecordsmanager.filestore;

import jakarta.persistence.Column;
import jakarta.persistence.Embeddable;
import jakarta.persistence.JoinColumn;
import jakarta.persistence.ManyToOne;

@Embeddable
public class FileStoreMiddlewareUsage {

    @ManyToOne(targetEntity = FileStoreMiddleware.class, optional = false)
    @JoinColumn(name = "middleware_id")
    public FileStoreMiddleware<?> middleware;

    @Column(name = "application_order")
    public int applicationOrder;

    public FileStoreMiddlewareUsage(FileStoreMiddleware<?> middleware, int order) {
        this.middleware = middleware;
        this.applicationOrder = order;
    }

    @Deprecated
    protected FileStoreMiddlewareUsage() {
    }
}
