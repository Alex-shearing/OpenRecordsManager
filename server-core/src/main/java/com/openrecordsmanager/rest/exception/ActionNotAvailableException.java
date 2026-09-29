package com.openrecordsmanager.rest.exception;

import com.openrecordsmanager.api.ResourceIdentifier;
import com.openrecordsmanager.api.errors.ApiException;

import java.util.UUID;

public class ActionNotAvailableException extends ApiException {
    public ActionNotAvailableException(ResourceIdentifier actionId, String targetType, UUID targetId) {
        super("action_not_available", actionId.toString(), targetType, targetId.toString());
    }
}
