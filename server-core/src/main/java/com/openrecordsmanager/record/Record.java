package com.openrecordsmanager.record;

import com.openrecordsmanager.api.ResourceIdentifier;
import com.openrecordsmanager.api.builtin.BuiltinProperties;
import com.openrecordsmanager.api.template.recordtype.SecurityFilterUsage;
import com.openrecordsmanager.filestore.store.FileStoreEntry;
import com.openrecordsmanager.plugin.ExpressionsService;
import com.openrecordsmanager.property.BuiltinPropertyMapper;
import com.openrecordsmanager.property.ObjectProperty;
import com.openrecordsmanager.property.ObjectPropertyHolder;
import com.openrecordsmanager.recordtype.RecordType;
import com.openrecordsmanager.recordtype.RecordTypeProperty;
import com.openrecordsmanager.user.User;
import jakarta.persistence.*;
import org.hibernate.annotations.JdbcTypeCode;
import org.hibernate.type.SqlTypes;
import org.jspecify.annotations.Nullable;

import java.time.Instant;
import java.util.*;
import java.util.stream.Collectors;

@Entity
@Table(name = "record")
@SuppressWarnings({"NotNullFieldNotInitialized", "CanBeFinal"})
public class Record implements ObjectPropertyHolder<RecordPropertyValue<?>> {
    private static final Map<ResourceIdentifier, BuiltinPropertyMapper<Record, ?>> BUILTIN_PROPERTY_MAPPERS = Map.ofEntries(
            Map.entry(BuiltinProperties.TITLE_ID, BuiltinPropertyMapper.of(Record::getTitle, (record, v) -> record.setTitle(Objects.requireNonNull(v)))),
            Map.entry(BuiltinProperties.NOTES_ID, BuiltinPropertyMapper.of(Record::getNotes, Record::setNotes)),
            Map.entry(BuiltinProperties.DATE_CREATED_ID, BuiltinPropertyMapper.of(Record::getDateCreated, (record, v) -> record.setDateCreated(Objects.requireNonNull(v)))),
            Map.entry(BuiltinProperties.DATE_REGISTERED_ID, BuiltinPropertyMapper.of(Record::getDateRegistered, Record::setDateRegistered)),
            Map.entry(BuiltinProperties.DATE_MODIFIED_ID, BuiltinPropertyMapper.of(Record::getDateModified, (record, v) -> record.setDateModified(Objects.requireNonNull(v)))),
            Map.entry(BuiltinProperties.KEYWORDS_ID, BuiltinPropertyMapper.of(Record::getKeywords, Record::setKeywords)),
            Map.entry(BuiltinProperties.MIME_TYPES_ID, BuiltinPropertyMapper.of(Record::getMimeTypes, Record::setMimeTypes))
    );

    @Id
    private UUID id;

    @Column(nullable = false)
    private String title;

    @ManyToOne(optional = false)
    @JoinColumn(nullable = false)
    private RecordType type;

    @Column
    @Lob
    @Nullable
    private String notes;

    @Column(nullable = false)
    private Instant dateCreated;

    @Column
    @Nullable
    private Instant dateRegistered;

    @Column(nullable = false)
    private Instant dateModified;

    @Column
    @Lob
    @Nullable
    private String keywords;

    @Column
    @JdbcTypeCode(SqlTypes.JSON)
    @Nullable
    private List<String> mimeTypes;

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
        this.type = type;
        type.properties.forEach(p -> {
            this.setPropertyUntyped(p.property, p.getDefault());
        });
        this.title = title;
        this.dateCreated = Instant.now();
        this.dateModified = Instant.now();
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

    public @Nullable String getNotes() {
        return this.notes;
    }

    public void setNotes(@Nullable String notes) {
        this.notes = notes;
    }

    public Instant getDateCreated() {
        return this.dateCreated;
    }

    public void setDateCreated(Instant dateCreated) {
        this.dateCreated = dateCreated;
    }

    public @Nullable Instant getDateRegistered() {
        return this.dateRegistered;
    }

    public void setDateRegistered(@Nullable Instant dateRegistered) {
        this.dateRegistered = dateRegistered;
    }

    public Instant getDateModified() {
        return this.dateModified;
    }

    public void setDateModified(Instant dateModified) {
        this.dateModified = dateModified;
    }

    public @Nullable String getKeywords() {
        return this.keywords;
    }

    public void setKeywords(@Nullable String keywords) {
        this.keywords = keywords;
    }

    public @Nullable List<String> getMimeTypes() {
        return this.mimeTypes;
    }

    public void setMimeTypes(@Nullable List<String> mimeTypes) {
        this.mimeTypes = mimeTypes;
    }

    public RecordType getType() {
        return this.type;
    }

    public void setType(RecordType type) {
        this.type = type;
        Map<ObjectProperty<?>, RecordPropertyValue<?>> oldProperties = Map.copyOf(this.properties);

        this.properties.clear();
        type.properties.forEach(p -> this.setPropertyUntyped(p.property, p.getDefault()));

        oldProperties.forEach((property, holder) -> {
            if (this.canSetProperty(property)) {
                this.setPropertyUntyped(property, holder.value);
            }
        });
    }

    public void touchDateModified() {
        this.dateModified = Instant.now();
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

    @Override
    public Set<ObjectProperty<?>> getPropertyKeys() {
        return this.type.properties.stream()
                .map(prop -> prop.property)
                .collect(Collectors.toSet());
    }

    @Override
    public boolean canSetProperty(ObjectProperty<?> property) {
        return this.type.properties.stream().anyMatch(prop -> prop.property.equals(property));
    }

    @Override
    public <K> @Nullable K getProperty(ObjectProperty<K> property) {
        BuiltinPropertyMapper<Record, ?> builtinGetter = BUILTIN_PROPERTY_MAPPERS.get(property.getId());

        if (builtinGetter != null) {
            return property.getType().cast(builtinGetter.get(this));
        } else {
            RecordPropertyValue<?> value = this.properties.get(property);
            if (value == null) {
                return null;
            }
            return property.getType().cast(value.value);
        }
    }

    @Override
    public <K> void setProperty(ObjectProperty<K> property, @Nullable K value) {
        BuiltinPropertyMapper<Record, ?> builtinGetter = BUILTIN_PROPERTY_MAPPERS.get(property.getId());

        if (builtinGetter != null) {
            builtinGetter.set(this, value);
            return;
        }

        RecordPropertyValue<?> holder = this.properties.get(property);
        if (holder == null) {
            if (!this.canSetProperty(property)) {
                throw new IllegalArgumentException("Property " + property + " does not exist on object");
            }

            holder = this.createProperty(property, value);
            this.properties.put(property, holder);
        }

        holder.setValueUntyped(value);
    }

    public SecurityFilterUsage securityFilter(ExpressionsService expressions, User actor) {
        if (this.type.securityFilter != null && !expressions.checkPropertyExpression(
                this.id,
                this.type.securityFilter,
                null,
                actor,
                this)
        ) {
            return this.type.securityFilterUsage;
        }

        for (RecordTypeProperty<?> recordTypeProperty : this.type.properties) {
            ObjectProperty<?> property = recordTypeProperty.property;
            if (property.getSecurityFilter() == null) {
                continue;
            }

            Object value = this.getProperty(property);
            if (!expressions.checkPropertyExpression(this.id, property.getSecurityFilter(), value, actor, this)) {
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
