package com.openrecordsmanager.search;

import com.openrecordsmanager.api.ResourceIdentifier;
import com.openrecordsmanager.api.errors.ApiException;
import com.openrecordsmanager.api.search.SearchClause;
import com.openrecordsmanager.api.search.SearchFieldProvider;
import com.openrecordsmanager.api.search.SearchOperator;
import com.openrecordsmanager.api.types.ComponentTypes;
import com.openrecordsmanager.database.DataRepository;
import com.openrecordsmanager.plugin.registry.ComponentCatalog;
import com.openrecordsmanager.property.ObjectProperty;
import com.openrecordsmanager.search.sql.ObjectSearchSchema;
import com.openrecordsmanager.search.sql.ResolvedSQLClause;
import org.apache.commons.lang3.StringUtils;
import org.jspecify.annotations.Nullable;
import org.springframework.stereotype.Component;
import tools.jackson.databind.JsonNode;
import tools.jackson.databind.node.JsonNodeFactory;

import java.util.ArrayList;
import java.util.List;
import java.util.Optional;

/**
 * Expands default {@code q} text into clauses and validates property / plugin filter clauses.
 */
@Component
public class SearchCriteriaExpander {

    private final DataRepository repository;
    private final ComponentCatalog catalog;

    public SearchCriteriaExpander(DataRepository repository, ComponentCatalog catalog) {
        this.repository = repository;
        this.catalog = catalog;
    }

    public ExpandedCriteria expand(
            ObjectSearchSchema schema,
            @Nullable String q,
            @Nullable List<SearchClause> filters
    ) {
        List<ResolvedSQLClause> qClauses = new ArrayList<>();
        if (!StringUtils.isBlank(q)) {
            JsonNode value = JsonNodeFactory.instance.stringNode(q.trim());
            for (ResourceIdentifier field : schema.defaultSearchFields()) {
                qClauses.add(this.resolvePropertyClause(schema, new SearchClause(field, SearchOperator.LIKE, value)));
            }
        }

        List<ResolvedSQLClause> propertyClauses = new ArrayList<>();
        List<ResolvedPluginClause> pluginClauses = new ArrayList<>();

        if (filters != null) {
            for (SearchClause clause : filters) {
                // Get the filter provider
                Optional<SearchFieldProvider> provider = this.catalog.getRegistry(ComponentTypes.SEARCH_FIELD_PROVIDER)
                        .get(clause.field())
                        .filter(p -> p.getTarget() == schema.target());

                // This is a custom object search property
                if (provider.isPresent()) {
                    SearchFieldProvider fieldProvider = provider.get();
                    if (!fieldProvider.getSupportedOperators().contains(clause.op())) {
                        throw ApiException.validationFailed(clause.field().toString(), SearchOperatorSupport.OPERATOR_UNSUPPORTED, clause.op().name());
                    }
                    SearchOperatorSupport.validateValueShape(clause.field(), clause.op(), clause.value());
                    pluginClauses.add(new ResolvedPluginClause(clause, fieldProvider));
                    continue;
                }

                // This is an object property value search
                propertyClauses.add(this.resolvePropertyClause(schema, clause));
            }
        }

        return new ExpandedCriteria(List.copyOf(qClauses), List.copyOf(propertyClauses), List.copyOf(pluginClauses));
    }

    /**
     * Create a property clause for a generic object property
     *
     * @param schema the object schema to resolve for
     * @param clause the search clause
     * @return a resolved clause to search a property value
     */
    private ResolvedSQLClause resolvePropertyClause(ObjectSearchSchema schema, SearchClause clause) {
        ObjectSearchSchema.BuiltinColumn builtin = schema.builtinColumns().get(clause.field());
        if (builtin != null) {
            SearchOperatorSupport.validate(clause.field(), builtin.type(), clause.op(), clause.value());
            return new ResolvedSQLClause.ResolvedBuiltinPropertySQLClause(clause, builtin);
        }

        ObjectProperty<?> property = this.repository.objectPropertyRepo.findById(clause.field())
                .filter(p -> !p.isUserHidden())
                .orElseThrow(() -> ApiException.validationFailed(
                        clause.field().toString(),
                        SearchOperatorSupport.FIELD_UNSUPPORTED
                ));
        SearchOperatorSupport.validate(clause.field(), property.getType(), clause.op(), clause.value());
        return new ResolvedSQLClause.ResolvedExternalPropertySQLClause(clause, property);
    }

    public record ExpandedCriteria(
            List<ResolvedSQLClause> qClauses,
            List<ResolvedSQLClause> propertyFilters,
            List<ResolvedPluginClause> pluginFilters
    ) {
        public boolean isEmpty() {
            return this.qClauses.isEmpty() && this.propertyFilters.isEmpty() && this.pluginFilters.isEmpty();
        }
    }

    public record ResolvedPluginClause(SearchClause clause, SearchFieldProvider provider) {
    }
}
