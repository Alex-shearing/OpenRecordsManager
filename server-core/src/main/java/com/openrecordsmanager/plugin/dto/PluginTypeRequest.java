package com.openrecordsmanager.plugin.dto;

public enum PluginTypeRequest {
    ZIP("zip"),
    JAR("jar");

    private final String extension;

    PluginTypeRequest(String extension) {
        this.extension = extension;
    }

    @Override
    public String toString() {
        return this.extension;
    }
}
