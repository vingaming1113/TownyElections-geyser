package com.townyelections.geyser;

import java.util.UUID;
import java.lang.reflect.Method;
import java.util.logging.Level;

/**
 * TownyElections Geyser Extension
 * 
 * Provides native Bedrock Forms GUI for TownyElections.
 * This allows Bedrock players to use native Bedrock forms instead of 
 * the Java inventory GUI for all election interactions.
 * 
 * Requires Geyser 2.9.0+ for Forms API support.
 */
public class TownyElectionsExtension {

    private Object logger;
    private Object geyserAPI;
    private Object townyElectionsPlugin;
    private ElectionFormsManager formsManager;
    private TownyElectionsBridge bridge;
    
    // Reflection cached references
    private Class<?> extensionLoggerClass;
    private Class<?> geyserAPIClass;
    private Class<?> pluginClass;
    private Class<?> eventBusClass;
    private Class<?> subscribeAnnotationClass;
    private Class<?> bukkitClass;
    private Class<?> pluginManagerClass;

    public TownyElectionsExtension() {
    }

    public void onEnable(Object logger) {
        this.logger = logger;
        extensionLoggerClass = logger.getClass();
    }

    public void onPreInitialize(Object event) {
        try {
            geyserAPIClass = Class.forName("org.geysermc.api.GeyserAPI");
            Method getApi = event.getClass().getMethod("getApi");
            geyserAPI = getApi.invoke(event);
            log(Level.INFO, "TownyElections Geyser Extension pre-initializing...");
        } catch (Exception e) {
            log(Level.SEVERE, "Error in pre-initialize: " + e.getMessage(), e);
        }
    }

    public void onPostInitialize(Object event) {
        try {
            log(Level.INFO, "TownyElections Geyser Extension initializing...");
            
            // Load classes
            pluginClass = Class.forName("org.bukkit.plugin.Plugin");
            bukkitClass = Class.forName("org.bukkit.Bukkit");
            pluginManagerClass = Class.forName("org.bukkit.plugin.PluginManager");
            eventBusClass = Class.forName("org.geysermc.api.event.bus.EventBus");
            subscribeAnnotationClass = Class.forName("org.geysermc.api.event.Subscribe");
            
            // Check if TownyElections plugin is available
            Method getPluginManager = bukkitClass.getMethod("getPluginManager");
            Object pluginManager = getPluginManager.invoke(null);
            Method getPlugin = pluginManagerClass.getMethod("getPlugin", String.class);
            Object plugin = getPlugin.invoke(pluginManager, "TownyElections");
            
            if (plugin == null) {
                log(Level.SEVERE, "TownyElections plugin not found! The extension requires TownyElections to be installed.");
                return;
            }
            
            townyElectionsPlugin = plugin;
            Method getDescription = pluginClass.getMethod("getDescription");
            Object description = getDescription.invoke(plugin);
            Method getVersion = description.getClass().getMethod("getVersion");
            String version = (String) getVersion.invoke(description);
            log(Level.INFO, "Found TownyElections plugin v" + version);
            
            // Initialize the bridge and forms manager
            bridge = new TownyElectionsBridge(this, geyserAPI);
            bridge.initialize();
            
            formsManager = new ElectionFormsManager(this, geyserAPI, bridge);
            formsManager.initialize();
            
            log(Level.INFO, "TownyElections Geyser Extension enabled successfully!");
            log(Level.INFO, "Bedrock players will now see native Forms GUI for elections.");
        } catch (Exception e) {
            log(Level.SEVERE, "Error in post-initialize: " + e.getMessage(), e);
        }
    }

    public void onShutdown(Object event) {
        log(Level.INFO, "TownyElections Geyser Extension shutting down...");
        
        if (formsManager != null) {
            formsManager.shutdown();
        }
        
        if (bridge != null) {
            bridge.shutdown();
        }
        
        log(Level.INFO, "TownyElections Geyser Extension disabled.");
    }

    public void log(Level level, String message) {
        try {
            Method logMethod = extensionLoggerClass.getMethod("log", Level.class, String.class);
            logMethod.invoke(logger, level, message);
        } catch (Exception e) {
            System.err.println("[TownyElections-geyser] " + level + ": " + message);
        }
    }

    public void log(Level level, String message, Throwable throwable) {
        try {
            Method logMethod = extensionLoggerClass.getMethod("log", Level.class, String.class, Throwable.class);
            logMethod.invoke(logger, level, message, throwable);
        } catch (Exception e) {
            System.err.println("[TownyElections-geyser] " + level + ": " + message);
            throwable.printStackTrace();
        }
    }

    public Object getLogger() {
        return logger;
    }

    public Object getGeyserAPI() {
        return geyserAPI;
    }

    public Object getTownyElectionsPlugin() {
        return townyElectionsPlugin;
    }

    public ElectionFormsManager getFormsManager() {
        return formsManager;
    }

    public TownyElectionsBridge getBridge() {
        return bridge;
    }
}
