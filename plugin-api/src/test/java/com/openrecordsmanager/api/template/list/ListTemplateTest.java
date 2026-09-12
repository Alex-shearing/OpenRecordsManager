package com.openrecordsmanager.api.template.list;

import com.openrecordsmanager.api.template.TemplateComponent;
import org.junit.jupiter.api.Assertions;
import org.junit.jupiter.api.Test;

import java.io.IOException;
import java.io.InputStream;
import java.util.HashSet;

class ListTemplateTest {

    @Test
    void testDeserialisationFull() {
        ListTemplate list = load("test_list.json", ListTemplate.class);
        ListTemplate codeList = ListTemplate.builder("Test List")
                .entry("entry_1", "Entry 1", e -> e.index(1))
                .entry("entry_2", "Entry 2", e -> e.index(2))
                .build();

        Assertions.assertEquals(list.name(), codeList.name(), "Name should be equal");
        Assertions.assertEquals(list.defaultEntries().keySet(), codeList.defaultEntries().keySet(), "Default entry ids should be equal");
        Assertions.assertEquals(new HashSet<>(list.defaultEntries().values()), new HashSet<>(codeList.defaultEntries().values()), "Default entry values should be equal");

        Assertions.assertEquals(list, codeList, "Object should be equal");
    }

    private static <T extends TemplateComponent> T load(String resource, Class<T> type) {
        try (InputStream in = ListTemplateTest.class.getClassLoader().getResourceAsStream(resource)) {
            if (in == null) {
                throw new IllegalArgumentException("Missing resource: " + resource);
            }
            return TemplateComponent.fromJson(in, type);
        } catch (IOException e) {
            throw new RuntimeException("Failed to read " + resource, e);
        }
    }
}
