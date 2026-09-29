package com.openrecordsmanager.search;

import com.openrecordsmanager.api.ResourceIdentifier;
import com.openrecordsmanager.api.errors.ApiError;
import com.openrecordsmanager.api.errors.ApiException;
import com.openrecordsmanager.api.search.SearchOperator;
import com.openrecordsmanager.api.template.property.PropertyType;
import org.junit.jupiter.api.Test;
import tools.jackson.databind.node.JsonNodeFactory;

import java.util.List;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertThrows;

class SearchOperatorSupportTest {

    @Test
    void rejectsGtOnString() {
        ApiException ex = assertThrows(
                ApiException.class,
                () -> SearchOperatorSupport.validate(
                        ResourceIdentifier.valueOf("builtin:title"),
                        PropertyType.STRING,
                        SearchOperator.GT,
                        JsonNodeFactory.instance.stringNode("x")
                )
        );
        ApiError fieldError = ex.getFieldErrors().get("builtin:title");
        assertEquals(SearchOperatorSupport.OPERATOR_UNSUPPORTED, fieldError.code());
        assertEquals(List.of("GT"), fieldError.args());
    }

    @Test
    void allowsLikeOnString() {
        SearchOperatorSupport.validate(
                ResourceIdentifier.valueOf("builtin:title"),
                PropertyType.STRING,
                SearchOperator.LIKE,
                JsonNodeFactory.instance.stringNode("%x%")
        );
    }
}
