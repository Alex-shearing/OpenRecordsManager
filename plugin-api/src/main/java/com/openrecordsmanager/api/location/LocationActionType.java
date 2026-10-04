package com.openrecordsmanager.api.location;

import com.openrecordsmanager.api.Component;

/**
 * Defines an action that can be run against a location.
 * User- or group-specific availability should be enforced in {@link #isAvailable}.
 *
 * @param <I> typed input form for this action; use {@link Record} when the action takes no inputs
 */
public abstract class LocationActionType<I extends Record> implements Component {
    private final Class<I> inputClass;

    protected LocationActionType(Class<I> inputClass) {
        this.inputClass = inputClass;
    }

    public Class<I> getInputClass() {
        return this.inputClass;
    }

    /**
     * Whether this action is currently available for the given location and actor.
     *
     * @param context actor and target location context
     * @return {@code true} if the action may be offered and executed
     */
    public abstract boolean isAvailable(LocationActionContext context);

    /**
     * Runs the action against the target location.
     *
     * @param context actor and target location context
     * @param inputs  validated action inputs
     */
    public abstract void execute(LocationActionContext context, I inputs);
}
