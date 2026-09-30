package com.openrecordsmanager.api.template;

import com.openrecordsmanager.api.Component;
import com.openrecordsmanager.api.ComponentReference;
import tools.jackson.databind.DeserializationFeature;
import tools.jackson.databind.ObjectMapper;
import tools.jackson.databind.json.JsonMapper;
import tools.jackson.databind.module.SimpleModule;

import java.io.InputStream;

public interface TemplateComponent extends Component {

    ObjectMapper MAPPER = JsonMapper.builder()
            // Template JSON may omit primitives that builders default (e.g. list entry index → 0).
            .disable(DeserializationFeature.FAIL_ON_NULL_FOR_PRIMITIVES)
            .addModule(new SimpleModule()
                    .addKeyDeserializer(ComponentReference.class, new ComponentReference.RefKeyDeserializer())
            )
            .build();

    /**
     * Load a template from a JSON input stream.
     */
    static <T extends TemplateComponent> T fromJson(InputStream inputStream, Class<T> clazz) {
        return MAPPER.readValue(inputStream, clazz);
    }
}
