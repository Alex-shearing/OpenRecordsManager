package com.openrecordsmanager.search;

import com.openrecordsmanager.api.ResourceIdentifier;
import com.openrecordsmanager.api.search.SearchClause;
import com.openrecordsmanager.api.search.SearchContext;
import com.openrecordsmanager.api.search.SearchFieldTarget;
import com.openrecordsmanager.api.search.SearchMatchMode;
import com.openrecordsmanager.search.sql.ObjectSearchSchema;
import com.openrecordsmanager.search.sql.SqlSearchSupport;
import com.openrecordsmanager.user.User;
import org.jspecify.annotations.Nullable;
import org.springframework.stereotype.Component;

import java.util.*;
import java.util.stream.Collectors;
import java.util.stream.Stream;

/**
 * Shared expand → SQL → plugin-merge pipeline used by domain search use cases.
 */
@Component
public class ObjectSearchExecutor {

    private final SearchCriteriaExpander expander;
    private final SqlSearchSupport searchSupport;

    public ObjectSearchExecutor(SearchCriteriaExpander expander, SqlSearchSupport searchSupport) {
        this.expander = expander;
        this.searchSupport = searchSupport;
    }

    public List<UUID> searchIds(
            ObjectSearchSchema schema,
            User actor,
            @Nullable String q,
            @Nullable List<SearchClause> filters,
            SearchMatchMode matchMode,
            @Nullable ResourceIdentifier typeScope,
            @Nullable UUID afterId,
            int limit
    ) {
        SearchCriteriaExpander.ExpandedCriteria criteria = this.expander.expand(schema, q, filters);

        int fetchLimit = Math.max(limit, 1);
        // Over-fetch when plugins or post-filters may drop rows; callers may request more.
        List<UUID> sqlIds = this.searchSupport.findMatchingIds(
                schema,
                criteria,
                matchMode,
                typeScope,
                afterId,
                fetchLimit
        );

        if (criteria.pluginFilters().isEmpty()) {
            return sqlIds;
        }

        SearchContext context = new ActorSearchContext(actor, schema.target());
        List<Set<UUID>> pluginSets = new ArrayList<>(criteria.pluginFilters().size());
        for (SearchCriteriaExpander.ResolvedPluginClause pluginFilter : criteria.pluginFilters()) {
            pluginSets.add(pluginFilter.provider().findMatchingIds(pluginFilter.clause(), context));
        }

        return mergeWithPluginIds(matchMode, sqlIds, pluginSets, afterId, fetchLimit);
    }

    public String summarize(@Nullable String q, @Nullable List<SearchClause> filters) {
        return SearchOperatorSupport.summarize(q, filters);
    }

    /**
     * Intersect (ALL) or union (ANY) SQL candidates with plugin id sets; preserve ascending order.
     */
    static List<UUID> mergeWithPluginIds(
            SearchMatchMode mode,
            List<UUID> sqlIds,
            List<Set<UUID>> pluginIdSets,
            @Nullable UUID afterId,
            int limit
    ) {
        if (pluginIdSets.isEmpty()) {
            return filterAndLimit(sqlIds.stream(), afterId, limit);
        }

        Set<UUID> pluginMerged = mergePluginSets(mode, pluginIdSets);
        Stream<UUID> combinedStream;

        if (mode == SearchMatchMode.ALL) {
            combinedStream = sqlIds.stream().filter(pluginMerged::contains);
        } else {
            Set<UUID> union = new HashSet<>(sqlIds);
            union.addAll(pluginMerged);
            combinedStream = union.stream();
        }

        return filterAndLimit(combinedStream, afterId, limit);
    }

    private static Set<UUID> mergePluginSets(SearchMatchMode mode, List<Set<UUID>> pluginIdSets) {
        if (mode == SearchMatchMode.ALL) {
            return pluginIdSets.stream()
                    .reduce((set1, set2) -> {
                        Set<UUID> intersection = new HashSet<>(set1);
                        intersection.retainAll(set2);
                        return intersection;
                    })
                    .orElse(Set.of());
        }

        return pluginIdSets.stream()
                .flatMap(Set::stream)
                .collect(Collectors.toSet());
    }

    private static List<UUID> filterAndLimit(Stream<UUID> stream, @Nullable UUID afterId, int limit) {
        if (afterId != null) {
            stream = stream.filter(id -> id.compareTo(afterId) > 0);
        }
        return stream.sorted().limit(limit).toList();
    }

    private record ActorSearchContext(User actor, SearchFieldTarget target) implements SearchContext {
        @Override
        public UUID getActorId() {
            return this.actor.getId();
        }

        @Override
        public String getActorUsername() {
            return this.actor.getUsername();
        }

        @Override
        public SearchFieldTarget target() {
            return this.target;
        }
    }
}
