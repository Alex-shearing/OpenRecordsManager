package com.openrecordsmanager.api;

/**
 * Extension entry point discovered via {@link java.util.ServiceLoader}.
 */
public interface Plugin {
    void initialise(RegistrationContext registry);
}
