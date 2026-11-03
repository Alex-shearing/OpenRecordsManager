package com.openrecordsmanager.property;

import com.openrecordsmanager.api.ResourceIdentifier;
import com.openrecordsmanager.api.builtin.BuiltinPlugin;
import com.openrecordsmanager.api.builtin.BuiltinPropertyIds;
import com.openrecordsmanager.api.property.ManagedObjectKind;
import com.openrecordsmanager.api.template.property.PropertyType;
import com.openrecordsmanager.template.ManagedObjectType;
import jakarta.persistence.*;
import org.hibernate.annotations.JdbcTypeCode;
import org.hibernate.id.uuid.UuidVersion7Strategy;
import org.hibernate.type.SqlTypes;
import org.jspecify.annotations.Nullable;
import tools.jackson.databind.JsonNode;
import tools.jackson.databind.node.NullNode;

import java.time.Instant;
import java.util.*;
import java.util.stream.Collectors;

/**
 * Base typed domain object (record, user, group, …): identity, type, notes, timestamps,
 * and dynamic/builtin properties. Domain access uses {@code T}; wire/storage uses {@link JsonNode}.
 */
@Entity
@Table(name = "managed_object")
@Inheritance(strategy = InheritanceType.JOINED)
@DiscriminatorColumn(name = "object_kind", discriminatorType = DiscriminatorType.STRING, length = 32)
@SuppressWarnings({"NotNullFieldNotInitialized", "CanBeFinal"})
public abstract class ManagedObject<T extends ManagedObjectType> {

    @Id
    @BuiltinProperty(value = BuiltinPropertyIds.ID, readOnly = true)
    private UUID id;

    @ManyToOne(optional = false, fetch = FetchType.EAGER, targetEntity = ManagedObjectType.class)
    @JoinColumn(name = "type_id", nullable = false)
    private T type;

    @BuiltinProperty(value = BuiltinPropertyIds.NOTES, defaultSearch = true)
    @Column(name = "notes")
    @JdbcTypeCode(SqlTypes.LONG32VARCHAR)
    @Nullable
    private String notes;

    @BuiltinProperty(BuiltinPropertyIds.DATE_CREATED)
    @Column(name = "date_created", nullable = false)
    private Instant dateCreated;

    @BuiltinProperty(value = BuiltinPropertyIds.DATE_MODIFIED, readOnly = true)
    @Column(name = "date_modified", nullable = false)
    private Instant dateModified;

    @ElementCollection(fetch = FetchType.EAGER)
    @CollectionTable(
            name = "managed_object_property_value",
            joinColumns = @JoinColumn(name = "managed_object_id")
    )
    @MapKeyJoinColumn(name = "property_id")
    private Map<ObjectProperty<?>, ManagedObjectPropertyValue> properties = new HashMap<>();

    @Deprecated
    protected ManagedObject() {
    }

    protected ManagedObject(T type) {
        type.assertCompatibleWith(this.getKind());
        this.type = type;
        this.id = UuidVersion7Strategy.INSTANCE.generateUuid(null);
        this.dateCreated = Instant.now();
        this.dateModified = Instant.now();
        type.getEffectiveProperties().forEach(binding ->
                this.applyPropertyDefault(binding.getProperty(), binding.getDefault())
        );
    }

    /**
     * Applies a type-assigned default when present and the target builtin is writable.
     */
    protected final <K> void applyPropertyDefault(ObjectProperty<K> property, @Nullable JsonNode defaultValue) {
        if (defaultValue == null || defaultValue.isNull()) {
            return;
        }
        BuiltinPropertyBinding<?> binding = this.getBuiltinPropertyBindings().get(property.getId());
        if (binding != null && binding.readOnly()) {
            return;
        }
        this.setPropertyFromJson(property, defaultValue);
    }

    public abstract ManagedObjectKind getKind();

    public UUID getId() {
        return this.id;
    }

    public T getType() {
        return this.type;
    }

    /**
     * Changes type, reapplies type defaults, and restores dynamic values still allowed by the new type.
     */
    public void setType(T type) {
        type.assertCompatibleWith(this.getKind());
        Map<ObjectProperty<?>, ManagedObjectPropertyValue> oldProperties = Map.copyOf(this.getDynamicProperties());

        this.type = type;
        this.getDynamicProperties().clear();
        type.getEffectiveProperties().forEach(binding ->
                this.applyPropertyDefault(binding.getProperty(), binding.getDefault())
        );

        oldProperties.forEach((property, holder) -> {
            if (this.canSetProperty(property)) {
                this.setPropertyFromJson(property, holder.getStoredValue());
            }
        });

        this.touchDateModified();
    }

    public @Nullable String getNotes() {
        return this.notes;
    }

    public Instant getDateCreated() {
        return this.dateCreated;
    }

    public Instant getDateModified() {
        return this.dateModified;
    }

    public void touchDateModified() {
        this.dateModified = Instant.now();
    }

    public boolean canSetProperty(ObjectProperty<?> property) {
        return this.type.hasProperty(property) || property.isSystemProperty();
    }

    public Set<ObjectProperty<?>> getPropertyKeys() {
        return this.type.getObjectProperties();
    }

    public ManagedObjectPropertyValue createProperty(ObjectProperty<?> property, JsonNode value) {
        return new ManagedObjectPropertyValue(property, value);
    }

    protected Map<ObjectProperty<?>, ManagedObjectPropertyValue> getDynamicProperties() {
        return this.properties;
    }

    protected abstract Map<ResourceIdentifier, BuiltinPropertyBinding<?>> getBuiltinPropertyBindings();

    /**
     * API wire map (list values as id strings).
     */
    public final Map<String, JsonNode> toWireMap() {
        return new WirePropertyMap(this);
    }

    /**
     * Domain map for CEL (heterogeneous {@code T} values; erasure only at this DYN boundary).
     */
    public final Map<String, @Nullable Object> toDomainMap() {
        return new DomainPropertyMap(this);
    }

    final boolean hasBuiltin(ResourceIdentifier id) {
        return this.getBuiltinPropertyBindings().containsKey(id);
    }

    final @Nullable Object readBuiltin(ResourceIdentifier id) {
        BuiltinPropertyBinding<?> binding = this.getBuiltinPropertyBindings().get(id);
        return binding == null ? null : binding.get(this);
    }

    public <K> @Nullable K getProperty(ObjectProperty<K> property) {
        BuiltinPropertyBinding<?> binding = this.getBuiltinPropertyBindings().get(property.getId());

        if (binding != null) {
            @SuppressWarnings("unchecked")
            K value = (K) binding.get(this);
            return value;
        }

        ManagedObjectPropertyValue stored = this.getDynamicProperties().get(property);
        if (stored == null) {
            return null;
        }

        JsonNode raw = stored.getStoredValue();
        @SuppressWarnings("unchecked")
        K hydrated = (K) PropertyValueCodec.requireInstalled().hydrate(property, raw);
        return hydrated;
    }

    /**
     * Set a typed domain property. Persists as {@link JsonNode} via
     * {@link PropertyValueCodec#toStored}. Domain {@code null} removes the dynamic property row.
     */
    public final <K> void setProperty(ObjectProperty<K> property, @Nullable K value) {
        if (!this.canSetProperty(property)) {
            throw new IllegalArgumentException("Property " + property + " does not exist on object");
        }

        BuiltinPropertyBinding<?> binding = this.getBuiltinPropertyBindings().get(property.getId());
        if (binding != null) {
            K oldValue = this.getProperty(property);
            if (!Objects.equals(oldValue, value)) {
                binding.set(this, value);
                this.touchDateModified();
            }
            return;
        }

        K oldValue = this.getProperty(property);
        if (!Objects.equals(oldValue, value)) {
            this.touchDateModified();

            JsonNode stored = PropertyValueCodec.requireInstalled().toStored(property, value);

            if (stored.isNull()) {
                this.getDynamicProperties().remove(property);
                return;
            }

            ManagedObjectPropertyValue holder = this.getDynamicProperties().get(property);
            if (holder == null) {
                holder = this.createProperty(property, stored);
                this.getDynamicProperties().put(property, holder);
            } else {
                holder.setStoredValue(stored);
            }
        }
    }

    /**
     * Set from wire/storage {@link JsonNode}: parse once to domain, then store as JsonNode.
     */
    public final <K> @Nullable K setPropertyFromJson(ObjectProperty<K> property, @Nullable JsonNode value) {
        K newValue = PropertyValueCodec.requireInstalled().parse(property, value);
        this.setProperty(property, newValue);
        return newValue;
    }

    public interface ObjectPropertyValue {
        ObjectProperty<?> getProperty();

        JsonNode getStoredValue();

        void setStoredValue(JsonNode value);
    }

    private static final class WirePropertyMap extends AbstractMap<String, JsonNode> {
        private static final List<ResourceIdentifier> ALWAYS_ON_WIRE = List.of(
                new ResourceIdentifier(BuiltinPlugin.BUILTIN_PLUGIN_NAME, BuiltinPropertyIds.ID),
                new ResourceIdentifier(BuiltinPlugin.BUILTIN_PLUGIN_NAME, BuiltinPropertyIds.DATE_CREATED),
                new ResourceIdentifier(BuiltinPlugin.BUILTIN_PLUGIN_NAME, BuiltinPropertyIds.DATE_MODIFIED)
        );

        private final ManagedObject<?> holder;

        private WirePropertyMap(ManagedObject<?> holder) {
            this.holder = holder;
        }

        @Override
        public @Nullable JsonNode get(Object key) {
            if (!(key instanceof String keyString)) {
                return null;
            }
            return this.findProperty(keyString)
                    .map(property -> encode(property, this.holder.getProperty(property)))
                    .orElseGet(() -> this.encodeAlwaysOnBuiltin(keyString));
        }

        @Override
        public Set<Entry<String, JsonNode>> entrySet() {
            Set<Entry<String, JsonNode>> entries = this.holder.getPropertyKeys().stream()
                    .filter(entry -> !entry.isSystemProperty())
                    .map(entry -> new AbstractMap.SimpleEntry<>(
                            entry.getId().toString(),
                            encode(entry, this.holder.getProperty(entry))
                    ))
                    .collect(Collectors.toSet());

            for (ResourceIdentifier id : ALWAYS_ON_WIRE) {
                JsonNode value = this.encodeAlwaysOnBuiltin(id.toString());
                if (value != null && entries.stream().noneMatch(entry -> id.toString().equals(entry.getKey()))) {
                    entries.add(new AbstractMap.SimpleEntry<>(id.toString(), value));
                }
            }
            return entries;
        }

        private Optional<ObjectProperty<?>> findProperty(String keyString) {
            return this.holder.getPropertyKeys().stream()
                    .filter(property -> !property.isSystemProperty())
                    .filter(property -> property.getId().toString().equals(keyString))
                    .findFirst();
        }

        private @Nullable JsonNode encodeAlwaysOnBuiltin(String keyString) {
            for (ResourceIdentifier id : ALWAYS_ON_WIRE) {
                if (!id.toString().equals(keyString) || !this.holder.hasBuiltin(id)) {
                    continue;
                }
                Object value = this.holder.readBuiltin(id);
                return value == null ? NullNode.getInstance() : PropertyType.toTree(value);
            }
            return null;
        }

        private static JsonNode encode(ObjectProperty<?> property, @Nullable Object domainValue) {
            return PropertyValueCodec.requireInstalled().encode(property, domainValue);
        }
    }

    private static final class DomainPropertyMap extends AbstractMap<String, Object> {
        private final ManagedObject<?> holder;

        private DomainPropertyMap(ManagedObject<?> holder) {
            this.holder = holder;
        }

        @Override
        public @Nullable Object get(Object key) {
            if (!(key instanceof String keyString)) {
                return null;
            }
            return this.holder.getPropertyKeys().stream()
                    .filter(property -> property.getId().toString().equals(keyString))
                    .findFirst()
                    .map(this.holder::getProperty)
                    .orElse(null);
        }

        @Override
        @SuppressWarnings("DataFlowIssue")
        public Set<Entry<String, Object>> entrySet() {
            return this.holder.getPropertyKeys().stream()
                    .map(entry -> new AbstractMap.SimpleEntry<String, Object>(
                            entry.getId().toString(),
                            this.holder.getProperty(entry)
                    ))
                    .collect(Collectors.toSet());
        }
    }
}
