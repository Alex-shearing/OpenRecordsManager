package com.openrecordsmanager.api.template;

import com.openrecordsmanager.api.Component;
import com.openrecordsmanager.api.Plugin;
import com.openrecordsmanager.api.schema.JsonSchemaValidator;

import java.io.IOException;
import java.io.InputStream;

public interface TemplateComponent extends Component {

    String name();

    static <T extends TemplateComponent> T fromJson(String fileName, Class<T> clazz) {
        try (InputStream inputStream = Plugin.class
                .getClassLoader()
                .getResourceAsStream(fileName)) {

            if (inputStream == null) {
                throw new IllegalArgumentException("File not found in resources!");
            }

            return JsonSchemaValidator.MAPPER.readValue(inputStream, clazz);
        } catch (IOException e) {
            throw new RuntimeException(e);
        }
    }
}
