package com.openrecordsmanager.search.dto;

import com.openrecordsmanager.api.ResourceIdentifier;
import com.openrecordsmanager.api.search.SearchClause;
import com.openrecordsmanager.api.search.SearchMatchMode;
import org.jspecify.annotations.Nullable;

import java.util.List;
import java.util.UUID;

public record ObjectSearchRequest(
        @Nullable String q,
        @Nullable List<SearchClause> filters,
        @Nullable SearchMatchMode match,
        @Nullable ResourceIdentifier type,
        @Nullable Integer limit,
        @Nullable UUID cursor
) {
    public SearchMatchMode matchOrDefault() {
        return this.match == null ? SearchMatchMode.ALL : this.match;
    }

    public int limitOrDefault() {
        int value = this.limit == null ? 50 : this.limit;
        return Math.clamp(value, 1, 200);
    }
}
