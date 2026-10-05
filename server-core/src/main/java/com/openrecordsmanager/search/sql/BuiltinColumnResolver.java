package com.openrecordsmanager.search.sql;

import jakarta.persistence.EntityManagerFactory;
import org.hibernate.engine.spi.SessionFactoryImplementor;
import org.hibernate.metamodel.mapping.*;
import org.hibernate.metamodel.spi.MappingMetamodelImplementor;
import org.hibernate.persister.entity.EntityPersister;
import org.jspecify.annotations.Nullable;
import org.springframework.stereotype.Component;

/**
 * Resolves physical SQL table/column identifiers from the Hibernate mapping metamodel.
 */
@Component
public class BuiltinColumnResolver {

    private static final String PROPERTIES_ATTRIBUTE = "properties";
    private static final String PROPERTY_VALUE_ATTRIBUTE = "value";
    private static final String TYPE_ATTRIBUTE = "type";

    private final MappingMetamodelImplementor mappingMetamodel;

    public BuiltinColumnResolver(EntityManagerFactory entityManagerFactory) {
        SessionFactoryImplementor sessionFactory = entityManagerFactory.unwrap(SessionFactoryImplementor.class);
        this.mappingMetamodel = sessionFactory.getMappingMetamodel();
    }

    /**
     * @param column          the attribute's physical column
     * @param tablePrimaryKey primary key of the table that owns {@code column} (used for joins)
     */
    record ResolvedColumn(QualifiedSqlColumn column, QualifiedSqlColumn tablePrimaryKey) {
    }

    ResolvedColumn resolveColumn(Class<?> entityClass, String javaAttribute) {
        EntityPersister persister = entityPersister(entityClass);

        // Hibernate keeps the identifier outside AttributeMapping; resolve it via identifier metadata.
        if (javaAttribute.equals(persister.getIdentifierPropertyName())) {
            QualifiedSqlColumn id = idColumn(persister, entityClass);
            return new ResolvedColumn(id, id);
        }

        SelectableMapping selectable = requirePhysicalSelectable(
                requireAttribute(persister, entityClass, javaAttribute),
                entityClass,
                javaAttribute
        );
        String table = selectable.getContainingTableExpression();
        if (table == null || table.isBlank()) {
            table = tableName(persister, entityClass);
        }
        return new ResolvedColumn(
                new QualifiedSqlColumn(table, requireSelectableName(selectable, entityClass, javaAttribute)),
                idColumnForTable(entityClass, table)
        );
    }

    HolderTableMetadata holderTables(Class<?> entityClass) {
        EntityPersister persister = entityPersister(entityClass);
        PluralAttributeMapping properties = requirePlural(persister, entityClass);
        String eavTable = propertyValueTable(properties, entityClass);

        return new HolderTableMetadata(
                idColumn(persister, entityClass),
                holderFk(properties, entityClass, eavTable),
                propertyId(properties, entityClass, eavTable),
                propertyValue(properties, entityClass, eavTable),
                getTypeColumn(persister, entityClass)
        );
    }

    private EntityPersister entityPersister(Class<?> entityClass) {
        return this.mappingMetamodel.getEntityDescriptor(entityClass);
    }

    private QualifiedSqlColumn idColumnForTable(Class<?> entityClass, String table) {
        for (Class<?> type = entityClass; type != null && type != Object.class; type = type.getSuperclass()) {
            EntityPersister persister = this.mappingMetamodel.findEntityDescriptor(type);
            if (persister == null) {
                continue;
            }
            if (table.equals(tableName(persister, type))) {
                return idColumn(persister, type);
            }
        }
        throw new IllegalArgumentException(
                "Table '" + table + "' is not mapped by " + entityClass.getName()
                        + " or a superclass entity"
        );
    }

    private static String tableName(EntityPersister persister, Class<?> entityClass) {
        String tableName = persister.getTableName();
        if (tableName == null || tableName.isBlank()) {
            throw new IllegalStateException("Blank table name for " + entityClass.getName());
        }
        return tableName;
    }

    private static QualifiedSqlColumn idColumn(EntityPersister persister, Class<?> entityClass) {
        String[] columns = persister.getIdentifierColumnNames();
        if (columns == null || columns.length != 1) {
            throw new IllegalStateException(
                    "Expected a single identifier column on " + entityClass.getName()
                            + ", got " + (columns == null ? 0 : columns.length)
            );
        }
        String column = columns[0];
        if (column.isBlank()) {
            throw new IllegalStateException("Blank identifier column on " + entityClass.getName());
        }
        return new QualifiedSqlColumn(tableName(persister, entityClass), column);
    }

    /**
     * Join-column for a to-one association on the owning entity, or {@code null} if absent.
     */
    private static @Nullable QualifiedSqlColumn getTypeColumn(
            EntityPersister persister,
            Class<?> entityClass
    ) {
        AttributeMapping attribute = persister.findAttributeMapping(TYPE_ATTRIBUTE);
        if (attribute == null) {
            return null;
        }
        if (!(attribute instanceof Association association)) {
            throw new IllegalArgumentException(
                    "Attribute '" + TYPE_ATTRIBUTE + "' on " + entityClass.getName()
                            + " is not an association"
            );
        }

        ValuedModelPart keyPart = association.getForeignKeyDescriptor().getKeyPart();
        String column = singleColumnName(keyPart, entityClass, TYPE_ATTRIBUTE);

        // Find the table that physically owns the FK. With JOINED inheritance the
        // persister's root table is the concrete subclass (e.g. user_details), but type
        // lives on the parent table (location).
        String table = tableName(persister, entityClass);
        if (keyPart.getJdbcTypeCount() == 1) {
            String containing = keyPart.getSelectable(0).getContainingTableExpression();
            if (containing != null && !containing.isBlank()) {
                table = containing;
            }
        }
        return new QualifiedSqlColumn(table, column);
    }

    /**
     * Physical table for the holder's {@code properties} EAV map.
     */
    private static String propertyValueTable(
            PluralAttributeMapping properties,
            Class<?> entityClass
    ) {
        String tableName = properties.getCollectionDescriptor().getTableName();
        if (tableName == null || tableName.isBlank()) {
            throw new IllegalStateException(
                    "Blank property-value table for '" + PROPERTIES_ATTRIBUTE + "' on " + entityClass.getName()
            );
        }
        return tableName;
    }

    /**
     * FK from the EAV table back to the holder ({@code record_id} / {@code location_id}).
     */
    private static QualifiedSqlColumn holderFk(
            PluralAttributeMapping properties,
            Class<?> entityClass,
            String eavTable
    ) {
        return new QualifiedSqlColumn(
                eavTable,
                singleColumnName(properties.getKeyDescriptor().getKeyPart(), entityClass, PROPERTIES_ATTRIBUTE)
        );
    }

    /**
     * Map-key column on the EAV table ({@code property_id}).
     */
    private static QualifiedSqlColumn propertyId(
            PluralAttributeMapping properties,
            Class<?> entityClass,
            String eavTable
    ) {
        CollectionPart index = properties.getIndexDescriptor();
        if (index == null) {
            throw new IllegalStateException(
                    "No map-key mapping for '" + PROPERTIES_ATTRIBUTE + "' on " + entityClass.getName()
            );
        }
        return new QualifiedSqlColumn(
                eavTable,
                singleColumnName(index, entityClass, PROPERTIES_ATTRIBUTE + ".key")
        );
    }

    /**
     * JSON value column on the EAV table ({@code property_value}).
     */
    private static QualifiedSqlColumn propertyValue(
            PluralAttributeMapping properties,
            Class<?> entityClass,
            String eavTable
    ) {
        CollectionPart element = properties.getElementDescriptor();
        if (!(element instanceof EmbeddableValuedModelPart embeddable)) {
            throw new IllegalStateException(
                    "Expected an embeddable element for '" + PROPERTIES_ATTRIBUTE + "' on "
                            + entityClass.getName()
            );
        }
        String path = PROPERTIES_ATTRIBUTE + "." + PROPERTY_VALUE_ATTRIBUTE;
        AttributeMapping valueAttribute = embeddable.getEmbeddableTypeDescriptor()
                .findAttributeMapping(PROPERTY_VALUE_ATTRIBUTE);
        if (valueAttribute == null) {
            throw new IllegalStateException(
                    "No '" + PROPERTY_VALUE_ATTRIBUTE + "' attribute on '" + PROPERTIES_ATTRIBUTE
                            + "' element for " + entityClass.getName()
            );
        }
        return new QualifiedSqlColumn(
                eavTable,
                requireSelectableName(
                        requirePhysicalSelectable(valueAttribute, entityClass, path),
                        entityClass,
                        path
                )
        );
    }

    private static AttributeMapping requireAttribute(
            EntityPersister persister,
            Class<?> entityClass,
            String javaAttribute
    ) {
        AttributeMapping attribute = persister.findAttributeMapping(javaAttribute);
        if (attribute == null) {
            throw new IllegalArgumentException(
                    "No metamodel attribute '" + javaAttribute + "' on " + entityClass.getName()
            );
        }
        return attribute;
    }

    private static PluralAttributeMapping requirePlural(
            EntityPersister persister,
            Class<?> entityClass
    ) {
        AttributeMapping attribute = requireAttribute(persister, entityClass, PROPERTIES_ATTRIBUTE);
        if (!(attribute instanceof PluralAttributeMapping plural)) {
            throw new IllegalArgumentException(
                    "Attribute '" + PROPERTIES_ATTRIBUTE + "' on " + entityClass.getName()
                            + " is not a plural mapping"
            );
        }
        return plural;
    }

    private static SelectableMapping requirePhysicalSelectable(
            AttributeMapping attribute,
            Class<?> entityClass,
            String path
    ) {
        if (!(attribute instanceof SelectableMapping selectable) || selectable.isFormula()) {
            throw new IllegalArgumentException(
                    "Attribute '" + path + "' on " + entityClass.getName()
                            + " is not a physical column"
            );
        }
        return selectable;
    }

    private static String singleColumnName(
            ValuedModelPart part,
            Class<?> entityClass,
            String path
    ) {
        if (part.getJdbcTypeCount() != 1) {
            throw new IllegalStateException(
                    "Expected a single column for '" + path + "' on "
                            + entityClass.getName() + ", got " + part.getJdbcTypeCount()
            );
        }
        return requireSelectableName(part.getSelectable(0), entityClass, path);
    }

    private static String requireSelectableName(
            SelectableMapping selectable,
            Class<?> entityClass,
            String path
    ) {
        String column = selectable.getSelectableName();
        if (column == null || column.isBlank()) {
            throw new IllegalStateException(
                    "Blank selectable name for '" + path + "' on " + entityClass.getName()
            );
        }
        return column;
    }

    record HolderTableMetadata(
            QualifiedSqlColumn primaryKey,
            QualifiedSqlColumn holderFk,
            QualifiedSqlColumn propertyId,
            QualifiedSqlColumn propertyValue,
            @Nullable QualifiedSqlColumn typeColumn
    ) {
    }
}
