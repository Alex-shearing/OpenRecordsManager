package com.openrecordsmanager.record;

import com.openrecordsmanager.api.ResourceIdentifier;
import com.openrecordsmanager.api.builtin.BuiltinPropertyIds;
import com.openrecordsmanager.api.property.ManagedObjectKind;
import com.openrecordsmanager.api.template.recordtype.SecurityFilterUsage;
import com.openrecordsmanager.filestore.store.FileStoreEntry;
import com.openrecordsmanager.location.user.User;
import com.openrecordsmanager.plugin.ExpressionsService;
import com.openrecordsmanager.property.BuiltinProperty;
import com.openrecordsmanager.property.BuiltinPropertyBinding;
import com.openrecordsmanager.property.ManagedObject;
import com.openrecordsmanager.record.type.RecordType;
import jakarta.persistence.*;
import org.hibernate.annotations.JdbcTypeCode;
import org.hibernate.type.SqlTypes;
import org.jspecify.annotations.Nullable;

import java.time.Instant;
import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import java.util.stream.Collectors;

@Entity
@Table(name = "record")
@DiscriminatorValue("RECORD")
@PrimaryKeyJoinColumn(name = "id")
@SuppressWarnings("CanBeFinal")
public class Record extends ManagedObject<RecordType> {
    public static final Map<ResourceIdentifier, BuiltinPropertyBinding<?>> BUILTIN_PROPERTY_BINDINGS =
            BuiltinPropertyBinding.scan(Record.class);

    @BuiltinProperty(value = BuiltinPropertyIds.TITLE, defaultSearch = true)
    @Column(nullable = false)
    private String title;

    @BuiltinProperty(value = BuiltinPropertyIds.KEYWORDS, defaultSearch = true)
    @Column
    @JdbcTypeCode(SqlTypes.LONG32VARCHAR)
    @Nullable
    private String keywords;

    @BuiltinProperty(BuiltinPropertyIds.DATE_REGISTERED)
    @Column
    @Nullable
    private Instant dateRegistered;

    @OneToMany(cascade = CascadeType.ALL, mappedBy = "record", fetch = FetchType.LAZY)
    private List<RecordRevision> revisions = new ArrayList<>();

    @Deprecated
    protected Record() {
    }

    public Record(String title, RecordType type) {
        super(type);
        this.title = title;
    }

    @Override
    public ManagedObjectKind getKind() {
        return ManagedObjectKind.RECORD;
    }

    public String getTitle() {
        return this.title;
    }

    public void setTitle(String title) {
        this.title = title;
        this.touchDateModified();
    }

    public @Nullable Instant getDateRegistered() {
        return this.dateRegistered;
    }

    public @Nullable String getKeywords() {
        return this.keywords;
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
    protected Map<ResourceIdentifier, BuiltinPropertyBinding<?>> getBuiltinPropertyBindings() {
        return BUILTIN_PROPERTY_BINDINGS;
    }

    public SecurityFilterUsage securityFilter(ExpressionsService expressions, User actor) {
        return this.getType().securityFilter(expressions, this, actor);
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
