package com.openrecordsmanager.plugin.exception;

import com.openrecordsmanager.plugin.DiscoveredPlugin;
import com.openrecordsmanager.rest.exception.ResourceInUseException;

public class CannotDowngradePluginException extends ResourceInUseException {
    public CannotDowngradePluginException(DiscoveredPlugin plugin) {
        super("plugin_cannot_downgrade", plugin.id(), plugin.version().toString());
    }
}
