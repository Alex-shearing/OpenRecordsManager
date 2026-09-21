package com.openrecordsmanager.plugin.authlocal;

import com.openrecordsmanager.api.ComponentReference;
import com.openrecordsmanager.api.auth.InputAuthProviderType;
import com.openrecordsmanager.api.auth.UserAuthContext;
import com.openrecordsmanager.api.auth.UserAuthDetails;
import com.openrecordsmanager.api.template.TemplateComponent;
import io.swagger.v3.oas.annotations.media.Schema;
import jakarta.validation.constraints.NotBlank;
import org.jspecify.annotations.Nullable;
import org.mindrot.jbcrypt.BCrypt;

import java.util.Optional;
import java.util.Set;

public class LocalAuthProviderType extends InputAuthProviderType<Record, LocalAuthProviderType.LocalAuthInputs> {

    public LocalAuthProviderType() {
        super(Record.class, LocalAuthInputs.class);
    }

    public record LocalAuthInputs(
            @Schema(title = "Username") @NotBlank String username,
            @Schema(title = "Password", format = "password", accessMode = Schema.AccessMode.WRITE_ONLY)
            @NotBlank String password
    ) {
    }

    @Override
    public @Nullable UserAuthDetails authenticate(
            UserAuthContext context,
            Record settings,
            LocalAuthInputs inputs
    ) {
        Optional<String> hash = context.getUserProperty(inputs.username(), AuthLocalPlugin.PASSWORD_HASH_PROPERTY);
        if (hash.isEmpty()) {
            AuthLocalPlugin.LOGGER.info("Failed to validate password for user {} (no password set)", inputs.username());
            return null;
        }

        try {
            if (!BCrypt.checkpw(inputs.password(), hash.get())) {
                AuthLocalPlugin.LOGGER.info("Failed to validate password for user {} (incorrect password)", inputs.username());
                return null;
            }
        } catch (IllegalArgumentException e) {
            AuthLocalPlugin.LOGGER.warn("Invalid password hash found for user {} on auth_local:password_hash property", inputs.username());
            return null;
        }

        return new UserAuthDetails(inputs.username(), "");
    }

    @Override
    public Set<ComponentReference<? extends TemplateComponent>> getDependencies() {
        return Set.of(ComponentReference.of(AuthLocalPlugin.PASSWORD_HASH_PROPERTY));
    }
}
