package com.openrecordsmanager.search;

import com.openrecordsmanager.search.sql.SearchWildcard;
import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.assertEquals;

class SearchWildcardTest {

    @Test
    void wrapsPlainTextAsContains() {
        assertEquals("%hello%", SearchWildcard.toLikePattern("hello"));
    }

    @Test
    void translatesUserWildcards() {
        assertEquals("hel%lo_", SearchWildcard.toLikePattern("hel*lo?"));
    }

    @Test
    void escapesLikeMetacharacters() {
        assertEquals("%100\\%%", SearchWildcard.toLikePattern("100%"));
    }
}
