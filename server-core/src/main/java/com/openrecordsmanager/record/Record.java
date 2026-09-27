package com.openrecordsmanager.record;

import com.openrecordsmanager.api.ResourceIdentifier;
import com.openrecordsmanager.api.builtin.BuiltinPropertyIds;
import com.openrecordsmanager.api.template.recordtype.SecurityFilterUsage;
import com.openrecordsmanager.filestore.store.FileStoreEntry;
import com.openrecordsmanager.plugin.ExpressionsService;
import com.openrecordsmanager.property.BuiltinProperty;
import com.openrecordsmanager.property.BuiltinPropertyBinding;
import com.openrecordsmanager.property.ObjectProperty;
import com.openrecordsmanager.property.ObjectPropertyHolder;
import com.openrecordsmanager.recordtype.RecordType;
import com.openrecordsmanager.recordtype.RecordTypeProperty;
import com.openrecordsmanager.user.User;
import jakarta.persistence.*;
import org.hibernate.annotations.JdbcTypeCode;
import org.hibernate.id.uuid.UuidVersion7Strategy;
import org.hibernate.type.SqlTypes;
import org.jspecify.annotations.Nullable;
import tools.jackson.databind.JsonNode;

import java.time.Instant;
import java.util.*;
import java.util.stream.Collectors;

@Entity
@Table(name = "record")
@SuppressWarnings({"NotNullFieldNotInitialized", "CanBeFinal"})
public class Record extends ObjectPropertyHolder<Record, RecordPropertyValue> {
    public static final Map<ResourceIdentifier, BuiltinPropertyBinding<Record, ?>> BUILTIN_PROPERTY_BINDINGS =
            BuiltinPropertyBinding.scan(Record.class);

    @Id
    private UUID id;

    @BuiltinProperty(value = BuiltinPropertyIds.TITLE_ID, defaultSearch = true)
    @Column(nullable = false)
    private String title;

    @ManyToOne(optional = false)
    @JoinColumn(nullable = false)
    private RecordType type;

    @BuiltinProperty(value = BuiltinPropertyIds.KEYWORDS_ID, defaultSearch = true)
    @Column
    @JdbcTypeCode(SqlTypes.LONG32VARCHAR)
    @Nullable
    private String keywords;

    @BuiltinProperty(value = BuiltinPropertyIds.NOTES_ID, defaultSearch = true)
    @Column
    @JdbcTypeCode(SqlTypes.LONG32VARCHAR)
    @Nullable
    private String notes;

    @BuiltinProperty(BuiltinPropertyIds.DATE_CREATED_ID)
    @Column(nullable = false)
    private Instant dateCreated;

    @BuiltinProperty(BuiltinPropertyIds.DATE_REGISTERED_ID)
    @Column
    @Nullable
    private Instant dateRegistered;

    @BuiltinProperty(value = BuiltinPropertyIds.DATE_MODIFIED_ID, readOnly = true)
    @Column(nullable = false)
    private Instant dateModified;

    @BuiltinProperty(BuiltinPropertyIds.MIME_TYPES_ID)
    @Column
    @JdbcTypeCode(SqlTypes.JSON)
    @Nullable
    private List<String> mimeTypes;

    @OneToMany(cascade = CascadeType.ALL, mappedBy = "record", fetch = FetchType.LAZY)
    private List<RecordRevision> revisions = new ArrayList<>();

    @ElementCollection(fetch = FetchType.EAGER)
    @CollectionTable(
            name = "record_property_value",
            joinColumns = @JoinColumn(name = "record_id")
    )
    @MapKeyJoinColumn(name = "property_id")
    private Map<ObjectProperty<?>, RecordPropertyValue> properties = new HashMap<>();

    @Deprecated
    protected Record() {
    }

    public Record(String title, RecordType type) {
        this.id = UuidVersion7Strategy.INSTANCE.generateUuid(null);
        this.type = type;
        type.getProperties().forEach(p -> this.setPropertyFromJson(p.getProperty(), p.getDefault()));
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
        Map<ObjectProperty<?>, RecordPropertyValue> oldProperties = Map.copyOf(this.properties);

        this.properties.clear();
        type.getProperties()
                .forEach(p -> this.setPropertyFromJson(p.getProperty(), p.getDefault()));

        oldProperties.forEach((property, holder) -> {
            if (this.canSetProperty(property)) {
                this.setPropertyFromJson(property, holder.getStoredValue());
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
    public RecordPropertyValue createProperty(ObjectProperty<?> property, @Nullable JsonNode value) {
        return new RecordPropertyValue(property, value);
    }

    @Override
    public Set<ObjectProperty<?>> getPropertyKeys() {
        return this.type.getProperties().stream()
                .map(RecordTypeProperty::getProperty)
                .collect(Collectors.toSet());
    }

    @Override
    public boolean canSetProperty(ObjectProperty<?> property) {
        return this.type.hasProperty(property);
    }

    @Override
    protected Map<ObjectProperty<?>, RecordPropertyValue> getDynamicProperties() {
        return this.properties;
    }

    @Override
    protected Map<ResourceIdentifier, BuiltinPropertyBinding<Record, ?>> getBuiltinPropertyBindings() {
        return BUILTIN_PROPERTY_BINDINGS;
    }

    @Override
    protected Record self() {
        return this;
    }

    public SecurityFilterUsage securityFilter(ExpressionsService expressions, User actor) {
        return this.type.securityFilter(expressions, this, actor);
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
