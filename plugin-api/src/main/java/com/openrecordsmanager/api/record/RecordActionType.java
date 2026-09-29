package com.openrecordsmanager.api.record;

import com.openrecordsmanager.api.Component;
import com.openrecordsmanager.api.schema.JsonSchemaValidator;

import java.util.Map;

public abstract class RecordActionType<I extends Record> implements Component {
    private final Class<I> inputClass;

    protected RecordActionType(Class<I> inputClass) {
        this.inputClass = inputClass;
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
