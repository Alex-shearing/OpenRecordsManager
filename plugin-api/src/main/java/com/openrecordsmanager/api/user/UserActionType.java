package com.openrecordsmanager.api.user;

import com.openrecordsmanager.api.Component;

/**
 * Defines an action that can be run against a user.
 *
 * @param <I> typed input form for this action; use {@link Record} when the action takes no inputs
 */
public abstract class UserActionType<I extends Record> implements Component {
    private final Class<I> inputClass;

    protected UserActionType(Class<I> inputClass) {
        this.inputClass = inputClass;
    }

    public Class<I> getInputClass() {
        return this.inputClass;
    }

    /**
     * Whether this action is currently available for the given user and actor.
     *
     * @param context actor and target user context
     * @return {@code true} if the action may be offered and executed
     */
    public abstract boolean isAvailable(UserActionContext context);

    /**
     * Runs the action against the target user.
     *
     * @param context actor and target user context
     * @param inputs  validated action inputs
     */
    public abstract void execute(UserActionContext context, I inputs);
}
