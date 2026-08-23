package com.openrecordsmanager.api.action;

import com.openrecordsmanager.api.Component;
import com.openrecordsmanager.api.schema.JsonSchemaValidator;

import java.util.Map;

public abstract class RecordActionType<I extends Record> implements Component {
    private final Class<I> inputClass;
    private final String displayName;
    private final String description;

    protected RecordActionType(Class<I> inputClass, String displayName, String description) {
        this.inputClass = inputClass;
        this.displayName = displayName;
        this.description = description;
    }

    public String getDisplayName() {
        return this.displayName;
    }

    public String getDescription() {
        return this.description;
    }

    public Class<I> getInputClass() {
        return this.inputClass;
    }

    public abstract boolean isAvailable(RecordActionContext context);

    public abstract void execute(RecordActionContext context, I inputs);

    public final void executeUntyped(RecordActionContext context, Map<String, ?> inputs) {
        this.execute(context, JsonSchemaValidator.toRecord(this.inputClass, inputs));
    }
}
