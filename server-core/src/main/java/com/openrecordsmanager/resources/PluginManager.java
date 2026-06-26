package com.openrecordsmanager.resources;

import com.openrecordsmanager.api.BuiltinComponents;
import com.openrecordsmanager.api.Plugin;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Service;

import java.io.File;
import java.net.MalformedURLException;
import java.net.URL;
import java.net.URLClassLoader;
import java.util.*;

@Service
public class PluginManager {
    private static final Logger LOGGER = LoggerFactory.getLogger(PluginManager.class);

    private List<Plugin> plugins = Collections.emptyList();

    public PluginManager(Plugin... additionalPlugins) {
        File loc = new File("./plugins");
        if (!loc.exists() || !loc.isDirectory()) {
            LOGGER.warn("Plugin directory '{}' not found. Plugins will not be loaded.", loc.getName());
            return;
        }

        File[] jarList = loc.listFiles((_, name) -> name.endsWith(".jar"));
        if (jarList == null) return;

        URL[] urls = new URL[jarList.length];
        for (int i = 0; i < jarList.length; i++) {
            try {
                urls[i] = jarList[i].toURI().toURL();
            } catch (MalformedURLException e) {
                LOGGER.error("Failed to load URL for plugin file {}", jarList[i].getName());
                continue;
            }
            LOGGER.info("Found plugin JAR: {}", jarList[i].getName());
        }

        // Create an isolated ClassLoader so plugins don't corrupt Server Core classpath
        URLClassLoader ucl = new URLClassLoader(urls, this.getClass().getClassLoader());

        // Use ServiceLoader to discover implementations inside the JARs
        ServiceLoader<Plugin> loader = ServiceLoader.load(Plugin.class, ucl);

        List<Plugin> loadedPlugins = new ArrayList<>();
        loadedPlugins.add(new BuiltinComponents());
        loadedPlugins.addAll(Arrays.asList(additionalPlugins));

        // Initialize all the plugins
        for (Plugin plugin : loader) {
            LOGGER.info("Found plugin '{}', loading...", plugin.getName());
            loadedPlugins.add(plugin);
        }

        this.plugins = Collections.unmodifiableList(loadedPlugins);
    }

    public List<Plugin> getPlugins() {
        return plugins;
    }
}
