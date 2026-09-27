package com.openrecordsmanager.search;

import com.openrecordsmanager.api.search.SearchMatchMode;
import org.junit.jupiter.api.Test;

import java.util.List;
import java.util.Set;
import java.util.UUID;

import static org.junit.jupiter.api.Assertions.assertEquals;

class ObjectSearchExecutorMergeTest {

    @Test
    void allModeIntersectsPluginIds() {
        UUID a = UUID.fromString("00000000-0000-0000-0000-000000000001");
        UUID b = UUID.fromString("00000000-0000-0000-0000-000000000002");
        UUID c = UUID.fromString("00000000-0000-0000-0000-000000000003");

        List<UUID> merged = ObjectSearchExecutor.mergeWithPluginIds(
                SearchMatchMode.ALL,
                List.of(a, b, c),
                List.of(Set.of(a, b), Set.of(b, c)),
                null,
                10
        );

        assertEquals(List.of(b), merged);
    }

    @Test
    void anyModeUnionsPluginIds() {
        UUID a = UUID.fromString("00000000-0000-0000-0000-000000000001");
        UUID b = UUID.fromString("00000000-0000-0000-0000-000000000002");
        UUID c = UUID.fromString("00000000-0000-0000-0000-000000000003");

        List<UUID> merged = ObjectSearchExecutor.mergeWithPluginIds(
                SearchMatchMode.ANY,
                List.of(a),
                List.of(Set.of(b), Set.of(c)),
                null,
                10
        );

        assertEquals(List.of(a, b, c), merged);
    }
}
