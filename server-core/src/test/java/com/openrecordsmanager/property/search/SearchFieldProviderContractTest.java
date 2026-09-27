package com.openrecordsmanager.property.search;

import com.openrecordsmanager.api.ResourceIdentifier;
import com.openrecordsmanager.api.search.SearchClause;
import com.openrecordsmanager.api.search.SearchContext;
import com.openrecordsmanager.api.search.SearchFieldProvider;
import com.openrecordsmanager.api.search.SearchFieldTarget;
import com.openrecordsmanager.api.search.SearchOperator;
import org.junit.jupiter.api.Test;

import java.util.Set;
import java.util.UUID;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertTrue;

class SearchFieldProviderContractTest {

    @Test
    void providerReturnsConfiguredTargetAndOps() {
        SearchFieldProvider provider = new SearchFieldProvider(
                "Content",
                "Full text content search",
                SearchFieldTarget.RECORD,
                Set.of(SearchOperator.LIKE, SearchOperator.EQ)
        ) {
            @Override
            public Set<UUID> findMatchingIds(SearchClause clause, SearchContext context) {
                return Set.of();
            }
        };

        assertEquals(SearchFieldTarget.RECORD, provider.getTarget());
        assertTrue(provider.getSupportedOperators().contains(SearchOperator.LIKE));
        assertEquals(
                Set.of(),
                provider.findMatchingIds(
                        new SearchClause(ResourceIdentifier.valueOf("test:content"), SearchOperator.LIKE, null),
                        new SearchContext() {
                            @Override
                            public UUID getActorId() {
                                return UUID.randomUUID();
                            }

                            @Override
                            public String getActorUsername() {
                                return "tester";
                            }

                            @Override
                            public SearchFieldTarget target() {
                                return SearchFieldTarget.RECORD;
                            }
                        }
                )
        );
    }
}
