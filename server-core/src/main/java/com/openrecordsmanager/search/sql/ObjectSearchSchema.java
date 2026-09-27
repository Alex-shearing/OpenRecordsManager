package com.openrecordsmanager.search.sql;

import com.openrecordsmanager.api.ResourceIdentifier;
import com.openrecordsmanager.api.search.SearchFieldTarget;
import com.openrecordsmanager.api.template.property.PropertyType;
import com.openrecordsmanager.property.BuiltinPropertyBinding;
import org.jspecify.annotations.Nullable;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

/**
 * Table / column metadata for SQL search against an {@code ObjectPropertyHolder} kind.
 * Built from entity bindings + Hibernate metamodel identifiers via {@link BuiltinColumnResolver}.
 */
public record ObjectSearchSchema(
        SearchFieldTarget target,
        String tableName,
        String idColumn,
        String propertyValueTable,
        String propertyValueFk,
        @Nullable String typeColumn,
        Map<ResourceIdentifier, BuiltinColumn> builtinColumns,
        List<ResourceIdentifier> defaultSearchFields
) {
    public static <T> ObjectSearchSchema of(
            SearchFieldTarget target,
            Class<T> entityClass,
            Map<ResourceIdentifier, BuiltinPropertyBinding<T, ?>> bindings,
            BuiltinColumnResolver columnResolver
    ) {
        BuiltinColumnResolver.HolderTableMetadata tables = columnResolver.holderTables(entityClass);
        Map<ResourceIdentifier, BuiltinColumn> columns = new HashMap<>();
        List<ResourceIdentifier> defaultSearchFields = new ArrayList<>();

        for (BuiltinPropertyBinding<?, ?> binding : bindings.values()) {
            String sqlColumn = columnResolver.sqlColumn(entityClass, binding.javaAttribute());
            columns.put(
                    binding.id(),
                    new BuiltinColumn(sqlColumn, binding.propertyType(), binding.jsonStored())
            );
            if (binding.defaultSearch()) {
                defaultSearchFields.add(binding.id());
            }
        }

        return new ObjectSearchSchema(
                target,
                tables.tableName(),
                tables.idColumn(),
                tables.propertyValueTable(),
                tables.propertyValueFk(),
                tables.typeColumn(),
                Map.copyOf(columns),
                List.copyOf(defaultSearchFields)
        );
    }

    public record BuiltinColumn(String sqlColumn, PropertyType<?> type, boolean jsonStored) {
    }
}
