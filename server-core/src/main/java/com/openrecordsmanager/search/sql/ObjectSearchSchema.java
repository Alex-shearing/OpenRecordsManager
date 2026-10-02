package com.openrecordsmanager.search.sql;

import com.openrecordsmanager.api.ResourceIdentifier;
import com.openrecordsmanager.api.search.SearchFieldTarget;
import com.openrecordsmanager.api.template.property.PropertyType;
import com.openrecordsmanager.property.BuiltinPropertyBinding;
import org.jspecify.annotations.Nullable;

import java.util.*;

/**
 * Table / column metadata for SQL search against an {@code ObjectPropertyHolder} kind.
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
            Map<ResourceIdentifier, ? extends BuiltinPropertyBinding<?, ?>> bindings,
            BuiltinColumnResolver columnResolver
    ) {
        BuiltinColumnResolver.HolderTableMetadata mainTable = columnResolver.holderTables(entityClass);

        Map<ResourceIdentifier, BuiltinColumn> columns = new HashMap<>();
        List<ResourceIdentifier> defaultSearchFields = new ArrayList<>();
        Set<QualifiedSqlColumn> joins = new LinkedHashSet<>();

        for (BuiltinPropertyBinding<?, ?> binding : bindings.values()) {
            BuiltinColumnResolver.ResolvedColumn resolved =
                    columnResolver.resolveColumn(entityClass, binding.javaAttribute());

            columns.put(
                    binding.id(),
                    new BuiltinColumn(resolved.column(), binding.propertyType(), binding.jsonStored())
            );

            if (!resolved.tablePrimaryKey().equals(mainTable.primaryKey())) {
                joins.add(resolved.tablePrimaryKey());
            }
            if (binding.defaultSearch()) {
                defaultSearchFields.add(binding.id());
            }
        }

        return new ObjectSearchSchema(
                target,
                mainTable.primaryKey(),
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
