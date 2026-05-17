package com.openrecordsmanager;

import java.io.File;
import java.net.URL;
import java.net.URLClassLoader;
import java.security.AuthProvider;
import java.util.ArrayList;
import java.util.List;
import java.util.ServiceLoader;

public class PluginManager {
    private final List<AuthProviderType> authProviders = new ArrayList<>();

    public void loadPluginsFromDirectory(String directoryPath) {
        File loc = new File(directoryPath);
        if (!loc.exists() || !loc.isDirectory()) {
            System.out.println("Plugin directory not found. Skipping dynamic load.");
            return;
        }

        File[] flist = loc.listFiles((dir, name) -> name.endsWith(".jar"));
        if (flist == null) return;

        try {
            URL[] urls = new URL[flist.length];
            for (int i = 0; i < flist.length; i++) {
                urls[i] = flist[i].toURI().toURL();
                System.out.println("Found plugin JAR: " + flist[i].getName());
            }

            // Create an isolated ClassLoader so plugins don't corrupt Server Core classpath
            URLClassLoader ucl = new URLClassLoader(urls, this.getClass().getClassLoader());

            // Use ServiceLoader to discover implementations inside the JARs
            ServiceLoader<Plugin> loader = ServiceLoader.load(Plugin.class, ucl);

            Plugin plugin = loader.findFirst().get();

            System.out.println(plugin.getName());

        } catch (Exception e) {
            System.err.println("Failed to load plugins cleanly: " + e.getMessage());
            e.printStackTrace();
        }
    }
}
