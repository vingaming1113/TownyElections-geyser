package com.townyelections.geyser;

import org.geysermc.api.GeyserAPI;
import org.geysermc.api.event.Subscribe;
import org.geysermc.api.event.bus.EventBus;
import org.geysermc.api.event.player.PlayerJoinEvent;
import org.geysermc.api.event.player.PlayerQuitEvent;

import org.bukkit.Bukkit;
import org.bukkit.entity.Player;
import org.bukkit.event.EventHandler;
import org.bukkit.event.Listener;
import org.bukkit.event.player.PlayerCommandSendEvent;

import com.townyelections.model.Election;
import com.townyelections.model.ElectionPhase;

import java.util.UUID;

/**
 * Listener for handling Bedrock player events related to TownyElections.
 * This class listens for Bedrock player joins, quits, and other events to
 * provide enhanced election functionality for Bedrock players.
 */
public class BedrockElectionListener implements Listener {

    private final TownyElectionsExtension extension;
    private final TownyElectionsBridge bridge;
    private final GeyserAPI geyserAPI;

    public BedrockElectionListener(TownyElectionsExtension extension, TownyElectionsBridge bridge) {
        this.extension = extension;
        this.bridge = bridge;
        this.geyserAPI = extension.getGeyserAPI();
    }

    public void register() {
        // Register Bukkit events
        Bukkit.getPluginManager().registerEvents(this, extension.getTownyElectionsPlugin());
        
        // Register Geyser events
        EventBus eventBus = geyserAPI.eventBus();
        eventBus.subscribe(this);
        
        extension.logger().info("BedrockElectionListener registered");
    }

    public void unregister() {
        // Unregister Bukkit events
        PlayerCommandSendEvent.getHandlerList().unregister(this);
        
        extension.logger().info("BedrockElectionListener unregistered");
    }

    @Subscribe
    public void onBedrockPlayerJoin(PlayerJoinEvent event) {
        UUID playerId = event.getPlayer().getUuid();
        
        // Check if this is a Bedrock player
        if (!geyserAPI.isBedrockPlayer(playerId)) {
            return;
        }

        extension.logger().info("Bedrock player joined: " + event.getPlayer().getUsername());
        
        // Notify the player about any active elections in their town
        notifyPlayerOfActiveElections(event.getPlayer());
    }

    @Subscribe
    public void onBedrockPlayerQuit(PlayerQuitEvent event) {
        UUID playerId = event.getPlayer().getUuid();
        
        // Check if this is a Bedrock player
        if (!geyserAPI.isBedrockPlayer(playerId)) {
            return;
        }

        extension.logger().info("Bedrock player left: " + event.getPlayer().getUsername());
    }

    @EventHandler
    public void onPlayerCommandSend(PlayerCommandSendEvent event) {
        Player player = event.getPlayer();
        UUID playerId = player.getUniqueId();
        
        // Only for Bedrock players
        if (!geyserAPI.isBedrockPlayer(playerId)) {
            return;
        }

        // Add Bedrock-specific election commands to the suggestion list
        addBedrockCommands(event);
    }

    /**
     * Notify a Bedrock player about any active elections in their town
     */
    private void notifyPlayerOfActiveElections(Player player) {
        if (!bridge.isInitialized()) {
            return;
        }

        // Get the player's town (this would be handled by Towny)
        // For now, we'll iterate through all active elections
        for (Election election : bridge.getActiveElections()) {
            if (election.getPhase() == ElectionPhase.VOTING || 
                election.getPhase() == ElectionPhase.RUNOFF) {
                // Send notification to the player
                player.sendMessage("§a[Election] §eThere is an active election in " + 
                                  election.getTownName() + "! Use /election to vote.");
            }
        }
    }

    /**
     * Add Bedrock-specific election commands to the command suggestion list
     */
    private void addBedrockCommands(PlayerCommandSendEvent event) {
        // Add Bedrock-specific commands if needed
        // This could include commands like /election vote, /election candidates, etc.
    }
}
