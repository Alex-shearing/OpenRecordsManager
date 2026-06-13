package com.openrecordsmanager.model;

import com.fasterxml.jackson.annotation.JsonProperty;
import com.openrecordsmanager.model.util.DbResourceIdentifier;
import com.openrecordsmanager.recordtype.RecordTypeDefinition;
import com.openrecordsmanager.resources.ResourceIdentifier;
import com.openrecordsmanager.resources.ResourceRegistry;
import jakarta.persistence.Column;
import jakarta.persistence.EmbeddedId;
import jakarta.persistence.Entity;
import jakarta.persistence.Table;
import org.hibernate.annotations.JdbcTypeCode;
import org.hibernate.type.SqlTypes;

import java.util.Set;
import java.util.stream.Collectors;

@Entity
@Table(name = "record_type")
public class RecordType {
    @EmbeddedId
    @JsonProperty
    public DbResourceIdentifier id;

    @Column(nullable = false)
    @JsonProperty
    public String name;

    @Column()
    @JsonProperty
    @JdbcTypeCode(SqlTypes.JSON)
    public Set<String> contentTypes;

    @Column(nullable = false)
    @JsonProperty
    @JdbcTypeCode(SqlTypes.JSON)
    public Set<ResourceIdentifier> properties;

    protected RecordType() {
    }

    public RecordType(ResourceIdentifier identifier) {
        this.id = new DbResourceIdentifier(identifier);
    }

    public boolean supportsFile() {
        return this.contentTypes != null && !this.contentTypes.isEmpty();
    }

    public RecordType fromDefinition(ResourceRegistry registry, RecordTypeDefinition definition) {
        this.name = definition.name();
        this.contentTypes = definition.allowedContentTypes();
        this.properties = definition.properties().stream().map(registry::getResourceId).collect(Collectors.toSet());

        return this;
    }
}
