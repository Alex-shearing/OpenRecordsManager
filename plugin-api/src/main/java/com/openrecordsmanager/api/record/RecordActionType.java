package com.openrecordsmanager.api.record;

import com.openrecordsmanager.api.Component;
import com.openrecordsmanager.api.action.ActionContext;

/**
 * Defines an action that can be run against a record.
 *
 * @param <I> typed input form for this action; use {@link Record} when the action takes no inputs
 */
public abstract class RecordActionType<I extends Record> implements Component {
    private final Class<I> inputClass;

    protected RecordActionType(Class<I> inputClass) {
        this.inputClass = inputClass;
    }

    public Class<I> getInputClass() {
        return this.inputClass;
    }

    /**
     * Whether this action is currently available for the given record and actor.
     *
     * @param context actor and target context
     * @return {@code true} if the action may be offered and executed
     */
    public abstract boolean isAvailable(ActionContext context);

    /**
     * Runs the action against the target record.
     *
     * @param context actor and target context
     * @param inputs  validated action inputs
     */
    public abstract void execute(ActionContext context, I inputs);
}
