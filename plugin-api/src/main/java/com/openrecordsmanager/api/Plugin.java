package com.openrecordsmanager.api;

public interface Plugin {
    String getName();

    void initialise(RegistrationContext registry);
}
