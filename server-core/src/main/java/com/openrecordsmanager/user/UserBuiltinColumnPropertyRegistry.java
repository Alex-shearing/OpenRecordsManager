package com.openrecordsmanager.user;

import com.openrecordsmanager.api.ResourceIdentifier;
import com.openrecordsmanager.api.builtin.BuiltinProperties;
import com.openrecordsmanager.database.DataRepository;
import com.openrecordsmanager.property.ObjectProperty;
import org.springframework.stereotype.Component;

import java.util.LinkedHashSet;
import java.util.Set;
import java.util.stream.Collectors;

@Component
public class UserBuiltinColumnPropertyRegistry {

    public static final Set<ResourceIdentifier> USER_COLUMN_PROPERTIES = Set.of(
            BuiltinProperties.DATE_CREATED_ID,
            BuiltinProperties.DATE_MODIFIED_ID,
            BuiltinProperties.GIVEN_NAME_ID,
            BuiltinProperties.SURNAME_ID,
            BuiltinProperties.HONORIFIC_ID,
            BuiltinProperties.EMAIL_ID
    );

    private static volatile Set<ObjectProperty<?>> userColumnPropertyKeys = Set.of();

    public void load(DataRepository repository) {
        userColumnPropertyKeys = USER_COLUMN_PROPERTIES.stream()
                .map(id -> repository.objectPropertyRepo.findById(id)
                        .orElseThrow(() -> new IllegalStateException("Missing builtin column property: " + id)))
                .collect(Collectors.toCollection(LinkedHashSet::new));
    }

    public static Set<ObjectProperty<?>> userColumnPropertyKeys() {
        return userColumnPropertyKeys;
    }

    static void initForTest(Set<ObjectProperty<?>> keys) {
        userColumnPropertyKeys = Set.copyOf(keys);
    }
}
