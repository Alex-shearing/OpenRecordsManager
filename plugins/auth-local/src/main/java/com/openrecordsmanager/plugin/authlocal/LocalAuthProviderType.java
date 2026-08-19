package com.openrecordsmanager.plugin.authlocal;

import com.openrecordsmanager.api.ComponentReference;
import com.openrecordsmanager.api.auth.AuthProviderInstance;
import com.openrecordsmanager.api.auth.InputAuthProviderType;
import com.openrecordsmanager.api.auth.UserAuthContext;
import com.openrecordsmanager.api.auth.UserAuthDetails;
import com.openrecordsmanager.api.config.ConfigStore;
import com.openrecordsmanager.api.template.TemplateComponent;
import org.jspecify.annotations.Nullable;

import java.util.Map;
import java.util.Optional;
import java.util.Set;

public class LocalAuthProviderType extends InputAuthProviderType {

    @Override
    public @Nullable UserAuthDetails authenticate(ConfigStore config, UserAuthContext context, AuthProviderInstance instance, Map<String, String> inputs) {
        if (!config.getOrDefault(AuthLocalPlugin.CONFIG_ADMIN_ENABLED, false)) {
            return null;
        }

        Optional<String> hash = context.getUserProperty(inputs.get("username"), AuthLocalPlugin.USER_PASSWORD_PROPERTY);
        if (hash.isEmpty()) {
            return null;
        }

        return new UserAuthDetails(instance, inputs.get("username"), "");
    }

    @Override
    public Set<ComponentReference<? extends TemplateComponent>> getDependencies() {
        return Set.of(ComponentReference.of(AuthLocalPlugin.USER_PASSWORD_PROPERTY));
    }
}
