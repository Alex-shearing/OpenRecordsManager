package com.openrecordsmanager.api.template;

import com.openrecordsmanager.api.Component;
import com.openrecordsmanager.api.ComponentReference;
import com.openrecordsmanager.api.Plugin;
import tools.jackson.databind.ObjectMapper;
import tools.jackson.databind.json.JsonMapper;
import tools.jackson.databind.module.SimpleModule;

import java.io.IOException;
import java.io.InputStream;

public interface TemplateComponent extends Component {
    ObjectMapper MAPPER = JsonMapper.builder()
            .addModule(new SimpleModule()
                    .addKeyDeserializer(ComponentReference.class, new ComponentReference.RefKeyDeserializer())
            )
            .build();

    static <T extends TemplateComponent> T fromJson(String fileName, Class<T> clazz) {
        try (InputStream inputStream = Plugin.class
                .getClassLoader()
                .getResourceAsStream(fileName)) {

            if (inputStream == null) {
                throw new IllegalArgumentException("File not found in resources!");
            }

            return MAPPER.readValue(inputStream, clazz);
        } catch (IOException e) {
            throw new RuntimeException(e);
        }
    }
}
