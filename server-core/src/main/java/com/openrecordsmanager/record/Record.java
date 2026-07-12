package com.openrecordsmanager.record;

import com.openrecordsmanager.api.template.recordtype.SecurityFilterUsage;
import com.openrecordsmanager.filestore.FileStoreEntry;
import com.openrecordsmanager.plugin.ExpressionsService;
import com.openrecordsmanager.property.ObjectProperty;
import com.openrecordsmanager.property.ObjectPropertyHolder;
import com.openrecordsmanager.recordtype.RecordType;
import com.openrecordsmanager.recordtype.RecordTypeProperty;
import com.openrecordsmanager.user.User;
import jakarta.persistence.*;
import org.jspecify.annotations.Nullable;
import tools.jackson.core.JacksonException;
import tools.jackson.core.JsonGenerator;
import tools.jackson.databind.SerializationContext;
import tools.jackson.databind.ValueSerializer;
import tools.jackson.databind.annotation.JsonSerialize;

import java.util.*;
import java.util.stream.Collectors;

@Entity
@Table(name = "record")
@JsonSerialize(using = Record.Serializer.class)
public class Record implements ObjectPropertyHolder<RecordPropertyValue<?>> {
    @Id
    public UUID id;

    @Column(nullable = false)
    public String title;

    @ManyToOne(optional = false)
    @JoinColumn(nullable = false)
    private RecordType type;

    @OneToMany(cascade = CascadeType.ALL, mappedBy = "record", fetch = FetchType.LAZY)
    public List<RecordRevision> revisions;

    @OneToMany(cascade = CascadeType.ALL, mappedBy = "record", fetch = FetchType.EAGER)
    @MapKey(name = "property")
    private Map<ObjectProperty<?>, RecordPropertyValue<?>> properties;

    @Deprecated
    protected Record() {
    }

    public Record(String title, RecordType type) {
        this.id = UUID.randomUUID();
        this.title = title;
        this.type = type;
        this.revisions = new ArrayList<>();
        if (this.type != null) {
            this.properties = type.properties.stream()
                    .map(prop -> Map.entry(prop.property, newProperty(prop)))
                    .collect(Collectors.toMap(Map.Entry::getKey, Map.Entry::getValue));
        } else {
            this.properties = new HashMap<>();
        }
    }

    public RecordType getType() {
        return this.type;
    }

    public void setType(RecordType type) {
        this.type = type;
        this.properties = type.properties.stream()
                .map(prop -> Map.entry(prop.property, newProperty(prop)))
                .collect(Collectors.toMap(Map.Entry::getKey, Map.Entry::getValue));
    }

    @Override
    public <V> RecordPropertyValue<V> createProperty(ObjectProperty<V> property, V value) {
        return new RecordPropertyValue<>(this, property, value);
    }

    private <T> RecordPropertyValue<T> newProperty(RecordTypeProperty<T> property) {
        RecordPropertyValue<?> oldValue = this.properties != null ? this.properties.get(property.property) : null;

        // Either get the previous value (if exists) or the property default
        T newValue = oldValue != null && oldValue.value != null ? property.property.type.cast(oldValue.value) : property.getDefault();

        return this.createProperty(property.property, newValue);
    }

    @Override
    public boolean canSetProperty(ObjectProperty<?> property) {
        return this.type == null || this.properties.containsKey(property);
    }

    @Override
    public Map<ObjectProperty<?>, RecordPropertyValue<?>> getProperties() {
        return this.properties;
    }

    public SecurityFilterUsage securityFilter(ExpressionsService expressions, User user, @Nullable Record record) {
        // Check record type filter
        if (this.type.securityFilter != null && !expressions.checkPropertyExpression(
                this.id,
                this.type.securityFilter,
                null,
                user,
                record)
        ) {
            return this.type.securityFilterUsage;
        }

        // Check all properties
        for (RecordPropertyValue<?> property : this.properties.values()) {
            if (!property.securityFilter(expressions, user, record)) {
                return this.type.securityFilterUsage;
            }
        }

        return SecurityFilterUsage.SHOW_ALL;
    }

    public void addRevision(double version, FileStoreEntry file) {
        if (this.revisions.stream().anyMatch(rev -> rev.version == version)) {
            throw new IllegalArgumentException("Revision already exists");
        }
        this.revisions.add(new RecordRevision(version, this, file));
    }

    public static class Serializer extends ValueSerializer<Record> {
        @Override
        public void serialize(Record value, JsonGenerator gen, SerializationContext ctxt) throws JacksonException {
            gen.writeStartObject();
            gen.writeStringProperty("id", value.id.toString());
            gen.writeStringProperty("title", value.title);
            gen.writeStringProperty("type", value.type.id.toString());

            gen.writeObjectPropertyStart("properties");
            value.properties.forEach((objectProperty, val) ->
                    gen.writePOJOProperty(objectProperty.id.toString(), val.getValue())
            );
            gen.writeEndObject();

            gen.writeEndObject();
        }
    }
}
