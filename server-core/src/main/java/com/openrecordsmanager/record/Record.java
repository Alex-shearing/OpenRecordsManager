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
public class Record extends ObjectPropertyHolder<Record, RecordPropertyValue<?>> {
    private static final Map<ResourceIdentifier, BuiltinPropertyMapper<Record, ?>> BUILTIN_PROPERTY_MAPPERS = Map.of(
            BuiltinProperties.TITLE_ID, BuiltinPropertyMapper.of(
                    Record::getTitle,
                    (r, v) -> r.title = Objects.requireNonNull(v)
            ),
            BuiltinProperties.NOTES_ID, BuiltinPropertyMapper.of(
                    Record::getNotes,
                    (r, v) -> r.notes = v
            ),
            BuiltinProperties.DATE_CREATED_ID, BuiltinPropertyMapper.of(
                    Record::getDateCreated,
                    (r, v) -> r.dateCreated = Objects.requireNonNull(v)
            ),
            BuiltinProperties.DATE_REGISTERED_ID, BuiltinPropertyMapper.of(
                    Record::getDateRegistered,
                    (r, v) -> r.dateRegistered = v
            ),
            BuiltinProperties.DATE_MODIFIED_ID, BuiltinPropertyMapper.of(
                    Record::getDateModified,
                    (_, _) -> {
                        throw new IllegalArgumentException("date modified cannot be set explicitly");
                    }
            ),
            BuiltinProperties.KEYWORDS_ID, BuiltinPropertyMapper.of(
                    Record::getKeywords,
                    (r, v) -> r.keywords = v
            ),
            BuiltinProperties.MIME_TYPES_ID, BuiltinPropertyMapper.of(
                    Record::getMimeTypes,
                    (r, v) -> r.mimeTypes = v
            )
    );

    @Id
    private UUID id;

    @Column(nullable = false)
    private String title;

    @ManyToOne(optional = false)
    @JoinColumn(nullable = false)
    private RecordType type;

    @Column
    @JdbcTypeCode(SqlTypes.LONG32VARCHAR)
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
    @JdbcTypeCode(SqlTypes.LONG32VARCHAR)
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

    public @Nullable String getNotes() {
        return this.notes;
    }

    public Instant getDateCreated() {
        return this.dateCreated;
    }

    public @Nullable Instant getDateRegistered() {
        return this.dateRegistered;
    }

    public Instant getDateModified() {
        return this.dateModified;
    }

    public void touchDateModified() {
        this.dateModified = Instant.now();
    }

    public @Nullable String getKeywords() {
        return this.keywords;
    }

    public @Nullable List<String> getMimeTypes() {
        return this.mimeTypes;
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

        this.touchDateModified();
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
    protected Map<ObjectProperty<?>, RecordPropertyValue<?>> getDynamicProperties() {
        return this.properties;
    }

    @Override
    protected Map<ResourceIdentifier, BuiltinPropertyMapper<Record, ?>> getBuiltinPropertyMappers() {
        return BUILTIN_PROPERTY_MAPPERS;
    }

    @Override
    protected Record self() {
        return this;
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
        this.touchDateModified();
        return recordRevision;
    }
}
