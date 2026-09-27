package com.openrecordsmanager.property.search;

import com.openrecordsmanager.api.ResourceIdentifier;
import com.openrecordsmanager.api.errors.InputValidationException;
import com.openrecordsmanager.api.search.SearchOperator;
import com.openrecordsmanager.api.template.property.PropertyType;
import com.openrecordsmanager.search.SearchOperatorSupport;
import org.junit.jupiter.api.Test;
import tools.jackson.databind.node.JsonNodeFactory;

import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;

class SearchOperatorSupportTest {

    @Test
    void rejectsGtOnString() {
        InputValidationException ex = assertThrows(
                InputValidationException.class,
                () -> SearchOperatorSupport.validate(
                        ResourceIdentifier.valueOf("builtin:title"),
                        PropertyType.STRING,
                        SearchOperator.GT,
                        JsonNodeFactory.instance.stringNode("x")
                )
        );
        assertTrue(ex.getFieldErrors().containsKey("builtin:title"));
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
