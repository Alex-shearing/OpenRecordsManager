package com.openrecordsmanager.api.template.list;

import com.openrecordsmanager.api.template.TemplateComponent;
import org.junit.jupiter.api.Assertions;
import org.junit.jupiter.api.Test;

import java.util.HashSet;

class ListTemplateTest {

    @Test
    void testDeserialisationFull() {
        ListTemplate list = TemplateComponent.fromJson("test_list.json", ListTemplate.class);
        ListTemplate codeList = ListTemplate.builder("Test List")
                .entry("entry_1", "Entry 1").index(1).endEntry()
                .entry("entry_2", "Entry 2").index(2).endEntry()
                .build();

        Assertions.assertEquals(list.name(), codeList.name(), "Name should be equal");
        Assertions.assertEquals(list.defaultEntries().keySet(), codeList.defaultEntries().keySet(), "Default entry ids should be equal");
        Assertions.assertEquals(new HashSet<>(list.defaultEntries().values()), new HashSet<>(codeList.defaultEntries().values()), "Default entry values should be equal");

        Assertions.assertEquals(list, codeList, "Object should be equal");
    }

}