package com.townyelections.geyser;

import org.geysermc.api.GeyserAPI;
import org.geysermc.api.extension.Extension;
import org.geysermc.api.extension.ExtensionLogger;
import org.geysermc.api.event.GeyserPreInitializeEvent;
import org.geysermc.api.event.GeyserPostInitializeEvent;
import org.geysermc.api.event.GeyserShutdownEvent;
import org.geysermc.api.event.Subscribe;
import org.geysermc.api.event.bus.EventBus;

import org.bukkit.Bukkit;
import org.bukkit.plugin.Plugin;

/**
 * TownyElections Geyser Extension
 * 
 * Provides native Bedrock Forms GUI for TownyElections.
 * This allows Bedrock players to use native Bedrock forms instead of 
 * the Java inventory GUI for all election interactions.
 * 
 * Requires Geyser 2.9.0+ for Forms API support.
 */
public class TownyElectionsExtension implements Extension {

    private ExtensionLogger logger;
    private GeyserAPI geyserAPI;
    private Plugin townyElectionsPlugin;
    private ElectionFormsManager formsManager;
    private TownyElectionsBridge bridge;

    @Override
    public ExtensionLogger logger() {
        return logger;
    }

    @Override
    public void onEnable(ExtensionLogger logger) {
        this.logger = logger;
    }

    @Subscribe
    public void onPreInitialize(GeyserPreInitializeEvent event) {
        geyserAPI = event.getApi();
        logger().info("TownyElections Geyser Extension pre-initializing...");
    }

    @Subscribe
    public void onPostInitialize(GeyserPostInitializeEvent event) {
        logger().info("TownyElections Geyser Extension initializing...");
        
        // Check if TownyElections plugin is available
        Plugin plugin = Bukkit.getPluginManager().getPlugin("TownyElections");
        if (plugin == null) {
            logger().severe("TownyElections plugin not found! The extension requires TownyElections to be installed.");
            return;
        }
        
        townyElectionsPlugin = plugin;
        logger().info("Found TownyElections plugin v" + plugin.getDescription().getVersion());
        
        // Initialize the bridge and forms manager
        bridge = new TownyElectionsBridge(this, geyserAPI);
        bridge.initialize();
        
        formsManager = new ElectionFormsManager(this, geyserAPI, bridge);
        formsManager.initialize();
        
        logger().info("TownyElections Geyser Extension enabled successfully!");
        logger().info("Bedrock players will now see native Forms GUI for elections.");
    }

    @Subscribe
    public void onShutdown(GeyserShutdownEvent event) {
        logger().info("TownyElections Geyser Extension shutting down...");
        
        if (formsManager != null) {
            formsManager.shutdown();
        }
        
        if (bridge != null) {
            bridge.shutdown();
        }
        
        logger().info("TownyElections Geyser Extension disabled.");
    }

    public GeyserAPI getGeyserAPI() {
        return geyserAPI;
    }

    public Plugin getTownyElectionsPlugin() {
        return townyElectionsPlugin;
    }

    public ElectionFormsManager getFormsManager() {
        return formsManager;
    }

    public TownyElectionsBridge getBridge() {
        return bridge;
    }
}
