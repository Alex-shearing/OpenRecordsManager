package com.openrecordsmanager.api;

public interface Plugin {
    String getName();

    void initialise(PluginContext registry);
}
