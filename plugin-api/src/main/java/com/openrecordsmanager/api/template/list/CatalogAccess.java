package com.openrecordsmanager.api.template.list;

import com.openrecordsmanager.api.ResourceIdentifier;

/**
 * List-scoped lookup for resolving wire element ids to {@link IListElement}.
 * Callers bind the list type when constructing the access.
 */
@FunctionalInterface
public interface CatalogAccess {
    IListElement resolveElement(ResourceIdentifier elementId);
}
