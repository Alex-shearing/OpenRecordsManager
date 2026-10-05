package com.openrecordsmanager.location;

import com.openrecordsmanager.api.audit.AuditEntityType;
import com.openrecordsmanager.audit.AuditService;
import com.openrecordsmanager.location.dto.LocationResponse;
import com.openrecordsmanager.location.dto.LocationSearchResponse;
import com.openrecordsmanager.location.user.User;
import com.openrecordsmanager.search.ObjectSearchExecutor;
import com.openrecordsmanager.search.dto.ObjectSearchRequest;
import com.openrecordsmanager.search.sql.ObjectSearchSchema;
import org.springframework.stereotype.Component;

import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import java.util.UUID;
import java.util.function.Function;

@Component
public class LocationSearchSupport {

    private final ObjectSearchExecutor searchExecutor;
    private final AuditService auditService;

    public LocationSearchSupport(ObjectSearchExecutor searchExecutor, AuditService auditService) {
        this.searchExecutor = searchExecutor;
        this.auditService = auditService;
    }

    public LocationSearchResponse search(
            ObjectSearchSchema schema,
            User actor,
            ObjectSearchRequest request,
            Function<List<UUID>, Map<UUID, ? extends Location>> loader
    ) {
        int pageLimit = request.limitOrDefault();
        List<UUID> ids = this.searchExecutor.searchIds(
                schema,
                actor,
                request.q(),
                request.filters(),
                request.matchOrDefault(),
                request.type(),
                request.cursor(),
                pageLimit + 1
        );

        boolean hasMore = ids.size() > pageLimit;
        if (hasMore) {
            ids = ids.subList(0, pageLimit);
        }

        Map<UUID, ? extends Location> loaded = loader.apply(ids);

        List<LocationResponse> items = new ArrayList<>(ids.size());
        for (UUID id : ids) {
            Location location = loaded.get(id);
            if (location != null) {
                items.add(LocationResponse.of(location));
            }
        }

        UUID nextCursor = hasMore && !items.isEmpty() ? items.getLast().id() : null;

        String scope = request.type() != null ? request.type().toString() : AuditService.COLLECTION_TARGET_ID;
        this.auditService.recordSearchRead(
                AuditEntityType.LOCATION,
                scope,
                this.searchExecutor.summarize(request.q(), request.filters()),
                items.size()
        );

        return new LocationSearchResponse(List.copyOf(items), nextCursor);
    }
}
