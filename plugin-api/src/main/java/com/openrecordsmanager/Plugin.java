package com.openrecordsmanager;

public interface Plugin {
    String getName();
    void initialise(PluginResourceRegistry registry);
}
