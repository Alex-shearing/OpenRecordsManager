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
@SuppressWarnings({"NotNullFieldNotInitialized", "CanBeFinal"})
public class Record implements ObjectPropertyHolder<RecordPropertyValue<?>> {
    @Id
    private UUID id;

    @Column(nullable = false)
    private String title;

    @ManyToOne(optional = false)
    @JoinColumn(nullable = false)
    private RecordType type;

    @OneToMany(cascade = CascadeType.ALL, mappedBy = "record", fetch = FetchType.LAZY)
    private List<RecordRevision> revisions = new ArrayList<>();

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
        this.properties = type.properties.stream()
                .map(prop -> Map.entry(prop.property, newProperty(prop, null)))
                .collect(Collectors.toMap(Map.Entry::getKey, Map.Entry::getValue));
    }

    public UUID getId() {
        return this.id;
    }

    public String getTitle() {
        return this.title;
    }

    public void setTitle(String title) {
        this.title = title;
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

    public RecordRevision getCurrentRevision() {
        return this.revisions.getLast();
    }

    public List<String> getRevisionList() {
        return this.revisions.stream()
                .map(RecordRevision::getVersion)
                .collect(Collectors.toList());
    }

    @Override
    public <V> RecordPropertyValue<V> createProperty(ObjectProperty<V> property, @Nullable V value) {
        return new RecordPropertyValue<>(this, property, value);
    }

    private <T> RecordPropertyValue<T> newProperty(RecordTypeProperty<T> property, @Nullable RecordPropertyValue<?> oldValue) {
        // Either get the previous value (if exists) or the property default
        T newValue = oldValue != null ? property.property.getType().cast(oldValue.value) : property.getDefault();

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

    public SecurityFilterUsage securityFilter(ExpressionsService expressions, User actor) {
        // Check record type filter
        if (this.type.securityFilter != null && !expressions.checkPropertyExpression(
                this.id,
                this.type.securityFilter,
                null,
                actor,
                this)
        ) {
            return this.type.securityFilterUsage;
        }

        // Check all properties
        for (RecordPropertyValue<?> property : this.properties.values()) {
            if (!property.securityFilter(expressions, actor, this)) {
                return this.type.securityFilterUsage;
            }
        }

        return SecurityFilterUsage.SHOW_ALL;
    }

    public RecordRevision addRevision(String version, FileStoreEntry file) {
        if (this.revisions.stream().anyMatch(rev -> rev.getVersion().equals(version))) {
            throw new IllegalArgumentException("Revision already exists");
        }
        RecordRevision recordRevision = new RecordRevision(version, this, file);
        this.revisions.add(recordRevision);
        return recordRevision;
    }
}
