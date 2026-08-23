package com.openrecordsmanager.auth.dto;

import com.openrecordsmanager.api.ResourceIdentifier;
import com.openrecordsmanager.api.auth.AuthProviderType;
import com.openrecordsmanager.api.types.ComponentType;
import com.openrecordsmanager.api.types.ComponentTypes;

import java.util.Map;

public record NewAuthProviderRequest(
        String name,
        ResourceIdentifier typeId,
        Type type,
        Map<String, Object> settings
) {
    public enum Type {
        INPUT(ComponentTypes.INPUT_AUTH_PROVIDER),
        REDIRECT(ComponentTypes.REDIRECT_AUTH_PROVIDER);

        public final ComponentType<? extends AuthProviderType> type;

        Type(ComponentType<? extends AuthProviderType> type) {
            this.type = type;
        }
    }
}
