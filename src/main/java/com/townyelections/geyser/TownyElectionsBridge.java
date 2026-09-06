package com.townyelections.geyser;

import org.geysermc.api.GeyserAPI;
import org.geysermc.api.connection.player.GeyserPlayer;

import org.bukkit.Bukkit;
import org.bukkit.plugin.Plugin;
import org.bukkit.entity.Player;

import com.townyelections.TownyElections;
import com.townyelections.manager.ElectionManager;
import com.townyelections.model.Election;
import com.townyelections.model.Candidate;
import com.townyelections.model.ElectionPhase;
import com.townyelections.model.OperationResult;

import java.util.UUID;
import java.util.Collection;
import java.util.Collections;
import java.util.List;
import java.util.ArrayList;
import java.util.Map;

import com.palmergames.bukkit.towny.object.Town;
import com.palmergames.bukkit.towny.object.Resident;
import com.townyelections.integration.Constituency;
import com.townyelections.integration.TownyHook;

/**
 * Bridge class that connects TownyElections functionality to GeyserMC.
 * Provides access to TownyElections data and operations for Bedrock players.
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
            Plugin plugin = extension.getTownyElectionsPlugin();
            if (plugin == null || !(plugin instanceof TownyElections)) {
                extension.logger().severe("Failed to cast plugin to TownyElections instance");
                return;
            }

            townyElections = (TownyElections) plugin;
            electionManager = townyElections.getElectionManager();
            townyHook = townyElections.getTownyHook();

            initialized = true;
            extension.logger().info("TownyElections bridge initialized successfully");

        } catch (Exception e) {
            extension.logger().severe("Error initializing TownyElections bridge: " + e.getMessage());
            e.printStackTrace();
        }
    }

    public void shutdown() {
        if (!initialized) {
            return;
        }

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
     * Get the GeyserPlayer for a player
     */
    public GeyserPlayer getGeyserPlayer(UUID playerId) {
        try {
            return (GeyserPlayer) geyserAPI.getConnection(playerId);
        } catch (Exception e) {
            extension.logger().warning("Error getting GeyserPlayer: " + e.getMessage());
            return null;
        }
    }

    /**
     * Get election for a specific town
     */
    public Election getElectionForTown(UUID townUuid) {
        if (electionManager == null) {
            return null;
        }
        return electionManager.getElection(townUuid);
    }

    /**
     * Get election for a player's town
     */
    public Election getElectionForPlayer(UUID playerId) {
        if (electionManager == null || townyHook == null) {
            return null;
        }

        Resident resident = townyHook.getResident(playerId);
        if (resident == null || !resident.hasTown()) {
            return null;
        }

        Town town = resident.getTownOrNull();
        if (town == null) {
            return null;
        }

        return electionManager.getElection(town.getUUID());
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
     * Check if a player can vote in their town's election
     */
    public boolean canVote(UUID playerId) {
        Election election = getElectionForPlayer(playerId);
        if (election == null) {
            return false;
        }

        if (election.getPhase() != ElectionPhase.VOTING && 
            election.getPhase() != ElectionPhase.RUNOFF) {
            return false;
        }

        return true;
    }

    /**
     * Check if a player has already voted
     */
    public boolean hasVoted(UUID playerId) {
        Election election = getElectionForPlayer(playerId);
        if (election == null) {
            return false;
        }
        return election.hasVoted(playerId);
    }

    /**
     * Get all candidates for a player's town election
     */
    public List<Candidate> getCandidates(UUID playerId) {
        Election election = getElectionForPlayer(playerId);
        if (election == null) {
            return Collections.emptyList();
        }
        return new ArrayList<>(election.getCandidateList());
    }

    /**
     * Get all candidates for a specific town
     */
    public List<Candidate> getCandidatesForTown(UUID townUuid) {
        Election election = getElectionForTown(townUuid);
        if (election == null) {
            return Collections.emptyList();
        }
        return new ArrayList<>(election.getCandidateList());
    }

    /**
     * Cast a vote for a Bedrock player
     */
    public boolean castVote(UUID playerId, UUID candidateId) {
        if (electionManager == null || townyHook == null) {
            return false;
        }

        try {
            Resident resident = townyHook.getResident(playerId);
            Player bukkitPlayer = Bukkit.getPlayer(playerId);
            
            if (resident == null || bukkitPlayer == null) {
                return false;
            }

            Town town = townyHook.getPlayerTown(bukkitPlayer);
            if (town == null) {
                return false;
            }

            Constituency constituency = townyHook.of(town);
            if (constituency == null) {
                return false;
            }

            OperationResult result = electionManager.castVote(resident, town, candidateId, bukkitPlayer);
            return result != null && result.success();
        } catch (Exception e) {
            extension.logger().warning("Error casting vote for Bedrock player: " + e.getMessage());
            return false;
        }
    }

    /**
     * Get election phase for a player's town
     */
    public ElectionPhase getElectionPhase(UUID playerId) {
        Election election = getElectionForPlayer(playerId);
        if (election == null) {
            return null;
        }
        return election.getPhase();
    }

    /**
     * Get time remaining for current election phase
     */
    public long getTimeRemaining(UUID playerId) {
        Election election = getElectionForPlayer(playerId);
        if (election == null) {
            return 0;
        }
        return election.getMillisRemaining();
    }

    /**
     * Get town name for a player
     */
    public String getTownName(UUID playerId) {
        if (townyHook == null) {
            return "Unknown";
        }

        Resident resident = townyHook.getResident(playerId);
        if (resident == null || !resident.hasTown()) {
            return "Unknown";
        }

        Town town = resident.getTownOrNull();
        return town != null ? town.getName() : "Unknown";
    }

    /**
     * Get candidate by UUID from any active election
     */
    public Candidate getCandidate(UUID candidateId) {
        if (electionManager == null) {
            return null;
        }
        
        for (Election election : electionManager.getActiveElections().values()) {
            Candidate candidate = election.getCandidate(candidateId);
            if (candidate != null) {
                return candidate;
            }
        }
        return null;
    }

    /**
     * Get total votes cast in election
     */
    public int getTotalVotes(UUID playerId) {
        Election election = getElectionForPlayer(playerId);
        if (election == null) {
            return 0;
        }
        return election.getTotalVotes();
    }

    /**
     * Get the vote tally for an election
     */
    public Map<UUID, Integer> getVoteTally(UUID playerId) {
        Election election = getElectionForPlayer(playerId);
        if (election == null) {
            return Collections.emptyMap();
        }
        return election.tally();
    }

    /**
     * Get the ballot for a specific player
     */
    public List<UUID> getBallot(UUID playerId) {
        Election election = getElectionForPlayer(playerId);
        if (election == null) {
            return Collections.emptyList();
        }
        return election.getBallot(playerId);
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
