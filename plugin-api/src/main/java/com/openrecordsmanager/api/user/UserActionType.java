package com.openrecordsmanager.api.user;

import com.openrecordsmanager.api.Component;
import com.openrecordsmanager.api.schema.JsonSchemaValidator;

import java.util.Map;

public abstract class UserActionType<I extends Record> implements Component {
    private final Class<I> inputClass;

    protected UserActionType(Class<I> inputClass) {
        this.inputClass = inputClass;
    }

    public Class<I> getInputClass() {
        return this.inputClass;
    }

    public abstract boolean isAvailable(UserActionContext context);

    public abstract void execute(UserActionContext context, I inputs);

    public final void executeUntyped(UserActionContext context, Map<String, ?> inputs) {
        this.execute(context, JsonSchemaValidator.toRecord(this.inputClass, inputs));
    }
}
