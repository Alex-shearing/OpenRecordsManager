package com.openrecordsmanager.api.action;

import com.openrecordsmanager.api.Component;

/**
 * Defines an action that can be run against a managed object.
 *
 * @param <I> typed input form for this action; use {@link Record} when the action takes no inputs
 */
public abstract class ActionType<I extends Record> implements Component {
    private final Class<I> inputClass;

    protected ActionType(Class<I> inputClass) {
        this.inputClass = inputClass;
    }

    public Class<I> getInputClass() {
        return this.inputClass;
    }

    /**
     * Whether this action is currently available for the given actor and target.
     * Defaults to always available; override to restrict by kind, properties, etc.
     */
    public boolean isAvailable(ActionContext context) {
        return true;
    }

    /**
     * Runs the action against the target.
     *
     * @param context actor and target context
     * @param inputs  validated action inputs
     */
    public abstract void execute(ActionContext context, I inputs);
}
