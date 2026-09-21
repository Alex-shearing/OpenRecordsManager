package com.openrecordsmanager.api.auth;

import com.openrecordsmanager.api.schema.JsonSchemaValidator;
import org.jspecify.annotations.Nullable;

import java.util.Map;

public abstract class InputAuthProviderType<S extends Record, I extends Record> extends AuthProviderType<S> {
    private final Class<I> inputClass;

    protected InputAuthProviderType(Class<S> settingsClass, Class<I> inputClass) {
        super(settingsClass);
        this.inputClass = inputClass;
    }

    /**
     * Attempt to authenticate a user with the provided credential input.
     *
     * @param context  usable context to pull user information
     * @param settings the configured instance settings
     * @param inputs   the user provided inputs
     * @return the {@link UserAuthDetails} or null if no user was authenticated
     */
    protected abstract @Nullable UserAuthDetails authenticate(
            UserAuthContext context,
            S settings,
            I inputs
    );

    public final @Nullable UserAuthDetails authenticateUntyped(
            UserAuthContext context,
            Map<String, ?> settings,
            Map<String, String> inputs
    ) {
        return this.authenticate(
                context,
                this.parseSettings(settings),
                JsonSchemaValidator.toRecord(this.getInputClass(), inputs)
        );
    }

    public Class<I> getInputClass() {
        return this.inputClass;
    }
}
