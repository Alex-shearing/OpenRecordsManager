package com.openrecordsmanager.record;

import com.openrecordsmanager.api.template.recordtype.SecurityFilterUsage;
import com.openrecordsmanager.filestore.store.FileStoreEntry;
import com.openrecordsmanager.plugin.ExpressionsService;
import com.openrecordsmanager.property.ObjectProperty;
import com.openrecordsmanager.property.ObjectPropertyHolder;
import com.openrecordsmanager.recordtype.RecordType;
import com.openrecordsmanager.recordtype.RecordTypeProperty;
import com.openrecordsmanager.user.User;
import jakarta.persistence.*;
import org.jspecify.annotations.Nullable;

import java.util.*;
import java.util.stream.Collectors;

@Entity
@Table(name = "record")
public class Record implements ObjectPropertyHolder<RecordPropertyValue<?>> {
    @Id
    public UUID id;

    @Column(nullable = false)
    public String title;

    @ManyToOne(optional = false)
    @JoinColumn(nullable = false)
    private RecordType type;

    @OneToMany(cascade = CascadeType.ALL, mappedBy = "record", fetch = FetchType.LAZY)
    public List<RecordRevision> revisions = new ArrayList<>();

    @OneToMany(cascade = CascadeType.ALL, mappedBy = "record", fetch = FetchType.EAGER)
    @MapKey(name = "property")
    private Map<ObjectProperty<?>, RecordPropertyValue<?>> properties = new HashMap<>();

    @Deprecated
    protected Record() {
    }

    public Record(String title, RecordType type) {
        this.id = UUID.randomUUID();
        this.title = title;
        this.type = type;
        this.revisions = new ArrayList<>();
        this.properties = type.properties.stream()
                .map(prop -> Map.entry(prop.property, newProperty(prop, null)))
                .collect(Collectors.toMap(Map.Entry::getKey, Map.Entry::getValue));
    }

    public RecordType getType() {
        return this.type;
    }

    public void setType(RecordType type) {
        this.type = type;
        this.properties = type.properties.stream()
                .map(prop -> Map.entry(prop.property, newProperty(prop, this.properties.get(prop.property))))
                .collect(Collectors.toMap(Map.Entry::getKey, Map.Entry::getValue));
    }

    @Override
    public <V> RecordPropertyValue<V> createProperty(ObjectProperty<V> property, @Nullable V value) {
        return new RecordPropertyValue<>(this, property, value);
    }

    private <T> RecordPropertyValue<T> newProperty(RecordTypeProperty<T> property, @Nullable RecordPropertyValue<?> oldValue) {
        // Either get the previous value (if exists) or the property default
        T newValue = oldValue != null ? property.property.type.cast(oldValue.value) : property.getDefault();

        return this.createProperty(property.property, newValue);
    }

    @Override
    public boolean canSetProperty(ObjectProperty<?> property) {
        return this.properties.containsKey(property);
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

    public void addRevision(String version, FileStoreEntry file) {
        if (this.revisions.stream().anyMatch(rev -> rev.version.equals(version))) {
            throw new IllegalArgumentException("Revision already exists");
        }
        this.revisions.add(new RecordRevision(version, this, file));
    }
}
