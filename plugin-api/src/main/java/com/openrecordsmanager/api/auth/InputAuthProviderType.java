package com.openrecordsmanager.api.auth;

import com.networknt.schema.Schema;
import com.openrecordsmanager.api.config.ConfigStore;
import com.openrecordsmanager.api.schema.JsonSchemaValidator;
import com.openrecordsmanager.api.schema.RecordInputs;
import org.jspecify.annotations.Nullable;

import java.util.Map;

public abstract class InputAuthProviderType<I extends Record> implements AuthProviderType {
    private final Class<I> inputClass;

    protected InputAuthProviderType(Class<I> inputClass) {
        this.inputClass = inputClass;
    }

    public final Schema getLoginInputSchema() {
        return JsonSchemaValidator.getSchema(this.inputClass);
    }

    /**
     * Attempt to authenticate a user with the provided credential input.
     *
     * @param context  usable context to pull user information
     * @param instance the instance of the authentication provider being used
     * @param inputs   the user provided inputs
     * @return the {@link UserAuthDetails} or null if no user was authenticated
     */
    public abstract @Nullable UserAuthDetails authenticate(
            ConfigStore config,
            UserAuthContext context,
            AuthProviderInstance instance,
            I inputs
    );

    public final @Nullable UserAuthDetails authenticateRaw(
            ConfigStore config,
            UserAuthContext context,
            AuthProviderInstance instance,
            Map<String, String> inputs
    ) {
        return this.authenticate(
                config,
                context,
                instance,
                this.inputClass.cast(RecordInputs.parse(this.inputClass, inputs))
        );
    }
}
