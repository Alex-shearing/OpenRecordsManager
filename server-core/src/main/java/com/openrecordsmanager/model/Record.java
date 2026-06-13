package com.openrecordsmanager.model;

import com.fasterxml.jackson.annotation.JsonProperty;
import com.openrecordsmanager.property.PropertyDefinition;
import com.openrecordsmanager.resources.ResourceIdentifier;
import com.openrecordsmanager.resources.ResourceRegistry;
import jakarta.persistence.*;
import org.hibernate.annotations.JdbcTypeCode;
import org.hibernate.type.SqlTypes;

import java.util.Map;
import java.util.UUID;

@Entity
@Table(name = "record")
public class Record {
    @Id
    @JsonProperty
    public UUID id;

    @Column(nullable = false)
    @JsonProperty
    public String title;

    @ManyToOne
    @JoinColumn(name = "type_id")
    @JsonProperty
    public RecordType type;

    @OneToOne(cascade = CascadeType.ALL)
    @JoinColumn(name = "file_id")
    @JsonProperty
    public FileStoreEntry file;

    @Column(nullable = false)
    @JsonProperty
    @JdbcTypeCode(SqlTypes.JSON)
    public Map<ResourceIdentifier, Object> properties;

    public Record() {
    }

    public <T> T getProperty(ResourceRegistry registry, ResourceIdentifier property) {
        Object value = this.properties.get(property);
        if (value == null) {
            return null;
        }

        PropertyDefinition<T> propDef = (PropertyDefinition<T>) registry.getProperties().get(property);
        return propDef.getType().validate(propDef, value);
    }
}
