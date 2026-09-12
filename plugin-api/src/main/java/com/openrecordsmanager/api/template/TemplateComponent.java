package com.openrecordsmanager.api.template;

import com.openrecordsmanager.api.Component;
import com.openrecordsmanager.api.schema.JsonSchemaValidator;

import java.io.InputStream;

public interface TemplateComponent extends Component {

    String name();

    /**
     * Load a template from a JSON input stream.
     */
    static <T extends TemplateComponent> T fromJson(InputStream inputStream, Class<T> clazz) {
        return JsonSchemaValidator.MAPPER.readValue(inputStream, clazz);
    }
}
