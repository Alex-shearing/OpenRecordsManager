package com.openrecordsmanager.search;

import com.openrecordsmanager.api.ResourceIdentifier;
import com.openrecordsmanager.search.dto.ObjectSearchRequest;
import com.openrecordsmanager.search.sql.ObjectSearchSchema;

import java.util.List;

/**
 * Resolves the effective display-column list for an object search.
 */
public final class ObjectSearchColumns {

    private ObjectSearchColumns() {
    }

    /**
     * When the request omits columns (or sends an empty list), use the schema's default search fields;
     * otherwise echo the requested property ids.
     */
    public static List<ResourceIdentifier> resolve(ObjectSearchRequest request, ObjectSearchSchema schema) {
        List<ResourceIdentifier> requested = request.columns();
        if (requested == null || requested.isEmpty()) {
            return schema.defaultSearchFields();
        }
        return List.copyOf(requested);
    }
}
