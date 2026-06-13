package com.openrecordsmanager.model.util;

import com.fasterxml.jackson.annotation.JsonValue;
import com.openrecordsmanager.resources.ResourceIdentifier;
import jakarta.persistence.Embeddable;

import java.io.Serializable;
import java.util.Objects;

@Embeddable
public class DbResourceIdentifier implements Serializable {

    private ResourceIdentifier id;

    protected DbResourceIdentifier() {
    }

    public DbResourceIdentifier(ResourceIdentifier id) {
        this.id = id;
    }

    public ResourceIdentifier getId() {
        return id;
    }

    @Override
    public boolean equals(Object o) {
        if (o == null || getClass() != o.getClass()) return false;
        DbResourceIdentifier that = (DbResourceIdentifier) o;
        return Objects.equals(id, that.id);
    }

    @Override
    public int hashCode() {
        return Objects.hashCode(id);
    }

    @Override
    @JsonValue
    public String toString() {
        return this.id.toString();
    }
}
