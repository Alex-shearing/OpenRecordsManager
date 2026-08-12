package com.openrecordsmanager.filestore.store;

import org.junit.jupiter.api.Test;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.web.util.UriComponentsBuilder;

import java.net.URI;
import java.net.URL;
import java.nio.file.Path;

import static org.junit.jupiter.api.Assertions.assertEquals;

@SpringBootTest
class FileStoreTest {

    @Test
    void testUriBuilder() {
        URI uri = URI.create("ABC/DEF/GHI.TXT?key=my_key");

        URI fromBuild = UriComponentsBuilder.newInstance()
                .path("ABC/DEF/GHI.TXT")
                .queryParam("key", "my_key")
                .build()
                .toUri();

        Path
        System.out.println(fromBuild.qu);

        assertEquals(uri, fromBuild, "URI constructed from builder is equal");
    }
}