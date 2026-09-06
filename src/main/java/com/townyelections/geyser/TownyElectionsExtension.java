package com.townyelections.geyser;

import org.geysermc.api.GeyserAPI;
import org.geysermc.api.event.GeyserPreInitializeEvent;
import org.geysermc.api.event.GeyserPostInitializeEvent;
import org.geysermc.api.event.GeyserShutdownEvent;
import org.geysermc.api.extension.Extension;
import org.geysermc.api.extension.ExtensionLogger;
import org.geysermc.api.event.Subscribe;

import org.bukkit.Bukkit;
import org.bukkit.plugin.Plugin;

/**
 * TownyElections Geyser Extension
 * 
 * This extension bridges TownyElections functionality to Bedrock Edition players
 * connecting through GeyserMC. It provides compatibility and enhanced features
 * for Bedrock players participating in town elections.
 */
public class TownyElectionsExtension implements Extension {

    private ExtensionLogger logger;
    private GeyserAPI geyserAPI;
    private Plugin townyElectionsPlugin;
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
        logger.info("TownyElections Geyser Extension pre-initializing...");
    }

    @Subscribe
    public void onPostInitialize(GeyserPostInitializeEvent event) {
        logger.info("TownyElections Geyser Extension initializing...");
        
        // Check if TownyElections plugin is available
        Plugin plugin = Bukkit.getPluginManager().getPlugin("TownyElections");
        if (plugin == null) {
            logger.severe("TownyElections plugin not found! The extension requires TownyElections to be installed.");
            return;
        }
        
        townyElectionsPlugin = plugin;
        logger.info("Found TownyElections plugin v" + plugin.getDescription().getVersion());
        
        // Initialize the bridge
        bridge = new TownyElectionsBridge(this, geyserAPI);
        bridge.initialize();
        
        logger.info("TownyElections Geyser Extension enabled successfully!");
    }

    @Subscribe
    public void onShutdown(GeyserShutdownEvent event) {
        logger.info("TownyElections Geyser Extension shutting down...");
        
        if (bridge != null) {
            bridge.shutdown();
        }
        
        logger.info("TownyElections Geyser Extension disabled.");
    }

    public GeyserAPI getGeyserAPI() {
        return geyserAPI;
    }

    public Plugin getTownyElectionsPlugin() {
        return townyElectionsPlugin;
    }

    public TownyElectionsBridge getBridge() {
        return bridge;
    }
}
