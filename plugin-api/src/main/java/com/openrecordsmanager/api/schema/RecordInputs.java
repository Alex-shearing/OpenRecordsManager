package com.openrecordsmanager.api.schema;

import com.openrecordsmanager.api.ComponentReference;
import tools.jackson.databind.ObjectMapper;
import tools.jackson.databind.json.JsonMapper;
import tools.jackson.databind.module.SimpleModule;

public final class RecordInputs {
    public static final ObjectMapper MAPPER = JsonMapper.builder()
            .addModule(new SimpleModule()
                    .addKeyDeserializer(ComponentReference.class, new ComponentReference.RefKeyDeserializer())
            )
            .build();

    private RecordInputs() {
    }

    public static <I extends Record> I parse(Class<I> recordClass, Object values) {
        return MAPPER.convertValue(values, recordClass);
    }
}
