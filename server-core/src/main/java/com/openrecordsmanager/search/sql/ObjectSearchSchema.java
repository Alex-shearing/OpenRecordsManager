package com.openrecordsmanager.search.sql;

import com.openrecordsmanager.api.ResourceIdentifier;
import com.openrecordsmanager.api.search.SearchFieldTarget;
import com.openrecordsmanager.api.template.property.PropertyType;
import com.openrecordsmanager.property.BuiltinPropertyBinding;
import org.jspecify.annotations.Nullable;

import java.util.*;

/**
 * Table / column metadata for SQL search against an {@code ManagedObject} kind.
 * Built from entity bindings + Hibernate metamodel identifiers via {@link BuiltinColumnResolver}.
 */
public record ObjectSearchSchema(
        SearchFieldTarget target,
        QualifiedSqlColumn primaryKey,
        List<QualifiedSqlColumn> joins,
        QualifiedSqlColumn holderFk,
        QualifiedSqlColumn propertyId,
        QualifiedSqlColumn propertyValue,
        @Nullable QualifiedSqlColumn typeColumn,
        Map<ResourceIdentifier, BuiltinColumn> builtinColumns,
        List<ResourceIdentifier> defaultSearchFields
) {
    public static ObjectSearchSchema of(
            SearchFieldTarget target,
            Class<?> entityClass,
            Map<ResourceIdentifier, BuiltinPropertyBinding<?>> bindings,
            BuiltinColumnResolver columnResolver
    ) {
        BuiltinColumnResolver.HolderTableMetadata mainTable = columnResolver.holderTables(entityClass);
        QualifiedSqlColumn primaryKey = mainTable.primaryKey();

        Map<ResourceIdentifier, BuiltinColumn> columns = new HashMap<>();
        List<ResourceIdentifier> defaultSearchFields = new ArrayList<>();
        Set<QualifiedSqlColumn> joins = new LinkedHashSet<>();

        for (BuiltinPropertyBinding<?> binding : bindings.values()) {
            BuiltinColumnResolver.ResolvedColumn resolved =
                    columnResolver.resolveColumn(entityClass, binding.javaAttribute());

            QualifiedSqlColumn column = resolved.column();
            QualifiedSqlColumn tablePrimaryKey = resolved.tablePrimaryKey();

            columns.put(
                    binding.id(),
                    new BuiltinColumn(column, binding.propertyType(), binding.jsonStored())
            );

            if (!tablePrimaryKey.equals(primaryKey)) {
                joins.add(tablePrimaryKey);
            }
            if (binding.defaultSearch()) {
                defaultSearchFields.add(binding.id());
            }
        }

        return new ObjectSearchSchema(
                target,
                primaryKey,
                List.copyOf(joins),
                mainTable.holderFk(),
                mainTable.propertyId(),
                mainTable.propertyValue(),
                mainTable.typeColumn(),
                Map.copyOf(columns),
                List.copyOf(defaultSearchFields)
        );
    }

    public record BuiltinColumn(
            QualifiedSqlColumn column,
            PropertyType<?> type,
            boolean jsonStored
    ) {
    }
}
