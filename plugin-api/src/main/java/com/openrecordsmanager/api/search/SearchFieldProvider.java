package com.openrecordsmanager.api.search;

import com.openrecordsmanager.api.Component;

import java.util.Set;
import java.util.UUID;

/**
 * Plugin-provided searchable field that is not backed by an {@code ObjectProperty}.
 *
 * <p>Example: full-text content search that calls an external index and returns matching
 * record/user ids. The host intersects or unions those ids with SQL property predicates.
 */
public abstract class SearchFieldProvider implements Component {
    private final String displayName;
    private final String description;
    private final SearchFieldTarget target;
    private final Set<SearchOperator> supportedOperators;

    protected SearchFieldProvider(
            String displayName,
            String description,
            SearchFieldTarget target,
            Set<SearchOperator> supportedOperators
    ) {
        this.displayName = displayName;
        this.description = description;
        this.target = target;
        this.supportedOperators = Set.copyOf(supportedOperators);
    }

    public final String getDisplayName() {
        return this.displayName;
    }

    public final String getDescription() {
        return this.description;
    }

    public final SearchFieldTarget getTarget() {
        return this.target;
    }

    public final Set<SearchOperator> getSupportedOperators() {
        return this.supportedOperators;
    }

    /**
     * Resolve matching holder ids for the clause. Called only when {@link SearchClause#field()}
     * equals this provider's registered {@link com.openrecordsmanager.api.ResourceIdentifier}.
     */
    public abstract Set<UUID> findMatchingIds(SearchClause clause, SearchContext context);
}
