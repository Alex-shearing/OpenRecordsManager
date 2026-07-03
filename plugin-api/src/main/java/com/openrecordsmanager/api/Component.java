package com.openrecordsmanager.api;

import tools.jackson.databind.ObjectMapper;

import java.io.IOException;
import java.io.InputStream;
import java.util.Set;

public interface Component {
    ObjectMapper MAPPER = new ObjectMapper();

    default Set<ComponentReference<? extends Component>> getDependencies() {
        return Set.of();
    }

    static <T extends Component> T fromJson(String fileName, Class<T> clazz) {
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
