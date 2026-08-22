package com.openrecordsmanager.plugin.authlocal;

import com.openrecordsmanager.api.ComponentReference;
import com.openrecordsmanager.api.auth.AuthProviderInstance;
import com.openrecordsmanager.api.auth.InputAuthProviderType;
import com.openrecordsmanager.api.auth.UserAuthContext;
import com.openrecordsmanager.api.auth.UserAuthDetails;
import com.openrecordsmanager.api.config.ConfigStore;
import com.openrecordsmanager.api.template.TemplateComponent;
import org.jspecify.annotations.Nullable;
import org.mindrot.jbcrypt.BCrypt;

import java.util.Map;
import java.util.Optional;
import java.util.Set;

public class LocalAuthProviderType extends InputAuthProviderType {

    @Override
    public @Nullable UserAuthDetails authenticate(ConfigStore config, UserAuthContext context, AuthProviderInstance instance, Map<String, String> inputs) {
        if (!config.getOrDefault(AuthLocalPlugin.CONFIG_ADMIN_ENABLED, false)) {
            return null;
        }

        String username = inputs.get("username");
        String password = inputs.get("password");

        if (username == null || password == null) {
            return null;
        }

        Optional<String> hash = context.getUserProperty(username, AuthLocalPlugin.PASSWORD_HASH_PROPERTY);
        if (hash.isEmpty()) {
            AuthLocalPlugin.LOGGER.info("Failed to validate password for user {} (no password set)", username);
            return null;
        }

        try {
            if (!BCrypt.checkpw(password, hash.get())) {
                AuthLocalPlugin.LOGGER.info("Failed to validate password for user {} (incorrect password)", username);
                return null;
            }
        } catch (IllegalArgumentException e) {
            AuthLocalPlugin.LOGGER.warn("Invalid password hash found for user {} on auth_local:password_hash property", username);
            return null;
        }

        return new UserAuthDetails(instance, username, "");
    }

    @Override
    public Set<ComponentReference<? extends TemplateComponent>> getDependencies() {
        return Set.of(ComponentReference.of(AuthLocalPlugin.PASSWORD_HASH_PROPERTY));
    }
}
