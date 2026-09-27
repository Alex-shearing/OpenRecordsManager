package com.openrecordsmanager.api.search;

import java.util.UUID;

/**
 * Host context passed to {@link SearchFieldProvider} implementations.
 */
public interface SearchContext {
    UUID getActorId();

    String getActorUsername();

    SearchFieldTarget target();
}
