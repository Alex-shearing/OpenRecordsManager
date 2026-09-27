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
    private static final String TYPE_ATTRIBUTE = "type";

    private final MappingMetamodelImplementor mappingMetamodel;

    public BuiltinColumnResolver(EntityManagerFactory entityManagerFactory) {
        SessionFactoryImplementor sessionFactory = entityManagerFactory.unwrap(SessionFactoryImplementor.class);
        this.mappingMetamodel = sessionFactory.getMappingMetamodel();
    }

    String sqlColumn(Class<?> entityClass, String javaAttribute) {
        AttributeMapping attribute = requireAttribute(entityPersister(entityClass), entityClass, javaAttribute);
        if (!(attribute instanceof SelectableMapping selectable) || selectable.isFormula()) {
            throw new IllegalArgumentException(
                    "Attribute '" + javaAttribute + "' on " + entityClass.getName()
                            + " is not a physical column"
            );
        }
        return requireSelectableName(selectable, entityClass, javaAttribute);
    }

    HolderTableMetadata holderTables(Class<?> entityClass) {
        EntityPersister persister = entityPersister(entityClass);
        PluralAttributeMapping properties = requirePlural(persister, entityClass);
        return new HolderTableMetadata(
                tableName(persister, entityClass),
                idColumn(persister, entityClass),
                collectionTable(properties, entityClass),
                collectionFkColumn(properties, entityClass),
                associationFkColumn(persister, entityClass)
        );
    }

    private EntityPersister entityPersister(Class<?> entityClass) {
        return this.mappingMetamodel.getEntityDescriptor(entityClass);
    }

    private static String tableName(EntityPersister persister, Class<?> entityClass) {
        String tableName = persister.getTableName();
        if (tableName == null || tableName.isBlank()) {
            throw new IllegalStateException("Blank table name for " + entityClass.getName());
        }
        return tableName;
    }

    private static String idColumn(EntityPersister persister, Class<?> entityClass) {
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
        return column;
    }

    /**
     * Join-column name for a to-one association on the owning entity, or {@code null} if absent.
     */
    private static @Nullable String associationFkColumn(
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
        return singleKeyColumn(association.getForeignKeyDescriptor(), entityClass, TYPE_ATTRIBUTE);
    }

    private static String collectionTable(
            PluralAttributeMapping plural,
            Class<?> entityClass
    ) {
        String tableName = plural.getCollectionDescriptor().getTableName();
        if (tableName == null || tableName.isBlank()) {
            throw new IllegalStateException(
                    "Blank collection table for '" + PROPERTIES_ATTRIBUTE + "' on " + entityClass.getName()
            );
        }
        return tableName;
    }

    private static String collectionFkColumn(
            PluralAttributeMapping plural,
            Class<?> entityClass
    ) {
        return singleKeyColumn(plural.getKeyDescriptor(), entityClass, PROPERTIES_ATTRIBUTE);
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

    private static String singleKeyColumn(
            ForeignKeyDescriptor foreignKey,
            Class<?> entityClass,
            String javaAttribute
    ) {
        ValuedModelPart keyPart = foreignKey.getKeyPart();
        if (keyPart.getJdbcTypeCount() != 1) {
            throw new IllegalStateException(
                    "Expected a single FK column for '" + javaAttribute + "' on "
                            + entityClass.getName() + ", got " + keyPart.getJdbcTypeCount()
            );
        }
        return requireSelectableName(keyPart.getSelectable(0), entityClass, javaAttribute);
    }

    private static String requireSelectableName(
            SelectableMapping selectable,
            Class<?> entityClass,
            String javaAttribute
    ) {
        String column = selectable.getSelectableName();
        if (column == null || column.isBlank()) {
            throw new IllegalStateException(
                    "Blank selectable name for '" + javaAttribute + "' on " + entityClass.getName()
            );
        }
        return column;
    }

    record HolderTableMetadata(
            String tableName,
            String idColumn,
            String propertyValueTable,
            String propertyValueFk,
            @Nullable String typeColumn
    ) {
    }
}
