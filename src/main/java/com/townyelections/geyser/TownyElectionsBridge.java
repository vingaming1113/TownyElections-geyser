package com.townyelections.geyser;

import org.geysermc.api.GeyserAPI;
import org.geysermc.api.event.bus.EventBus;

import org.bukkit.Bukkit;
import org.bukkit.plugin.Plugin;

import com.townyelections.TownyElections;
import com.townyelections.manager.ElectionManager;
import com.townyelections.model.Election;
import com.townyelections.model.Candidate;

import java.util.UUID;
import java.util.Collection;
import java.util.Collections;
import java.util.List;
import java.util.ArrayList;
import java.util.Map;
import java.util.HashMap;

import com.palmergames.bukkit.towny.object.Town;
import com.palmergames.bukkit.towny.object.Resident;
import com.townyelections.integration.Constituency;
import com.townyelections.integration.TownyHook;
import com.townyelections.model.ElectionPhase;
import com.townyelections.model.OperationResult;

/**
 * Bridge class that connects TownyElections functionality to GeyserMC.
 * This class handles the integration between the TownyElections plugin and
 * the Geyser extension system.
 */
public class TownyElectionsBridge {

    private final TownyElectionsExtension extension;
    private final GeyserAPI geyserAPI;
    private TownyElections townyElections;
    private ElectionManager electionManager;
    private TownyHook townyHook;
    private boolean initialized = false;

    public TownyElectionsBridge(TownyElectionsExtension extension, GeyserAPI geyserAPI) {
        this.extension = extension;
        this.geyserAPI = geyserAPI;
    }

    public void initialize() {
        if (initialized) {
            return;
        }

        try {
            // Get the TownyElections instance
            Plugin plugin = extension.getTownyElectionsPlugin();
            if (plugin == null || !(plugin instanceof TownyElections)) {
                extension.logger().severe("Failed to cast plugin to TownyElections instance");
                return;
            }

            townyElections = (TownyElections) plugin;
            electionManager = townyElections.getElectionManager();
            townyHook = townyElections.getTownyHook();

            // Register event listeners
            registerEventListeners();

            initialized = true;
            extension.logger().info("TownyElections bridge initialized successfully");

        } catch (Exception e) {
            extension.logger().severe("Error initializing TownyElections bridge: " + e.getMessage());
            e.printStackTrace();
        }
    }

    private void registerEventListeners() {
        // Register our custom event listeners
        EventBus eventBus = geyserAPI.eventBus();
        
        // Register Bedrock player election events
        BedrockElectionListener bedrockListener = new BedrockElectionListener(extension, this);
        bedrockListener.register();
        
        // Register Bedrock commands
        BedrockElectionCommands bedrockCommands = new BedrockElectionCommands(extension, this);
        bedrockCommands.initialize();
        eventBus.subscribe(bedrockCommands);
        
        extension.logger().info("Registered TownyElections event listeners and commands");
    }

    public void shutdown() {
        if (!initialized) {
            return;
        }

        // Clean up any resources
        initialized = false;
        extension.logger().info("TownyElections bridge shutdown complete");
    }

    /**
     * Check if a player is a Bedrock player (connected via Geyser)
     */
    public boolean isBedrockPlayer(UUID playerId) {
        return geyserAPI.isBedrockPlayer(playerId);
    }

    /**
     * Get election information for a specific town
     */
    public Election getElectionForTown(UUID townUuid) {
        if (electionManager == null) {
            return null;
        }
        return electionManager.getElection(townUuid);
    }

    /**
     * Get all active elections
     */
    public Collection<Election> getActiveElections() {
        if (electionManager == null) {
            return Collections.emptyList();
        }
        return electionManager.getActiveElections().values();
    }

    /**
     * Check if a player can vote in a specific election
     */
    public boolean canVote(UUID playerId, UUID townUuid) {
        if (electionManager == null) {
            return false;
        }
        
        Election election = electionManager.getElection(townUuid);
        if (election == null) {
            return false;
        }

        // Get the player's resident
        Resident resident = townyHook.getResident(playerId);
        if (resident == null) {
            return false;
        }

        // Check if the resident is part of the town
        Town town = townyHook.getTown(townUuid);
        if (town == null) {
            return false;
        }

        Constituency constituency = townyHook.of(town);
        if (constituency == null) {
            return false;
        }

        // Check if the resident is part of this constituency
        return constituency.isResident(resident.getUUID());
    }

    /**
     * Cast a vote for a Bedrock player
     */
    public boolean castVote(UUID playerId, UUID townUuid, UUID candidateId) {
        if (electionManager == null) {
            return false;
        }

        try {
            // Get the resident and town
            Resident resident = townyHook.getResident(playerId);
            Town town = townyHook.getTown(townUuid);
            
            if (resident == null || town == null) {
                return false;
            }

            Constituency constituency = townyHook.of(town);
            if (constituency == null) {
                return false;
            }

            // Use the election manager to cast the vote
            // We need to get the player for IP tracking, but for Bedrock players
            // we might not have a Bukkit Player object, so we'll pass null
            Player player = Bukkit.getPlayer(playerId);
            
            OperationResult result = electionManager.castVote(resident, town, candidateId, player);
            return result != null && result.success();
        } catch (Exception e) {
            extension.logger().warning("Error casting vote for Bedrock player: " + e.getMessage());
            return false;
        }
    }

    /**
     * Get candidate information for an election
     */
    public List<Candidate> getCandidates(UUID townUuid) {
        Election election = getElectionForTown(townUuid);
        if (election == null) {
            return Collections.emptyList();
        }
        return new ArrayList<>(election.getCandidateList());
    }

    public boolean isInitialized() {
        return initialized;
    }

    public TownyElections getTownyElections() {
        return townyElections;
    }

    public ElectionManager getElectionManager() {
        return electionManager;
    }

    public TownyHook getTownyHook() {
        return townyHook;
    }
}
