package com.openrecordsmanager.filestore.middleware;

import jakarta.persistence.Column;
import jakarta.persistence.Embeddable;
import jakarta.persistence.JoinColumn;
import jakarta.persistence.ManyToOne;

@Embeddable
@SuppressWarnings("NotNullFieldNotInitialized")
public class MiddlewareUsage {

    @ManyToOne(targetEntity = Middleware.class, optional = false)
    @JoinColumn(name = "middleware_id")
    public Middleware middleware;

    @Column(name = "application_order")
    public int applicationOrder;

    public MiddlewareUsage(Middleware middleware, int order) {
        this.middleware = middleware;
        this.applicationOrder = order;
    }

    @Deprecated
    protected MiddlewareUsage() {
    }
}
