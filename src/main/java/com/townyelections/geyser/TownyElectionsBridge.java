package com.townyelections.geyser;

import org.geysermc.api.GeyserAPI;
import org.geysermc.api.connection.player.GeyserPlayer;
import org.bukkit.Bukkit;
import org.bukkit.entity.Player;

import java.util.*;
import java.lang.reflect.Method;
import java.util.logging.Level;

/**
 * Bridge class that connects TownyElections functionality to GeyserMC using reflection.
 * This avoids compile-time dependencies on TownyElections and Towny.
 */
public class TownyElectionsBridge {

    private final TownyElectionsExtension extension;
    private final GeyserAPI geyserAPI;
    
    // Reflection cached references
    private Class<?> townyElectionsClass;
    private Class<?> electionManagerClass;
    private Class<?> electionClass;
    private Class<?> candidateClass;
    private Class<?> electionPhaseClass;
    private Class<?> townyHookClass;
    private Class<?> townClass;
    private Class<?> residentClass;
    private Class<?> constituencyClass;
    private Class<?> operationResultClass;
    
    private Object townyElections;
    private Object electionManager;
    private Object townyHook;
    private boolean initialized = false;

    public TownyElectionsBridge(TownyElectionsExtension extension, GeyserAPI geyserAPI) {
        this.extension = extension;
        this.geyserAPI = geyserAPI;
    }

    @SuppressWarnings("unchecked")
    public void initialize() {
        if (initialized) {
            return;
        }

        try {
            // Get TownyElections plugin
            org.bukkit.plugin.Plugin plugin = extension.getTownyElectionsPlugin();
            if (plugin == null) {
                extension.logger().severe("TownyElections plugin not found");
                return;
            }
            townyElections = plugin;
            townyElectionsClass = plugin.getClass();

            // Load classes by name
            electionManagerClass = Class.forName("com.townyelections.manager.ElectionManager");
            electionClass = Class.forName("com.townyelections.model.Election");
            candidateClass = Class.forName("com.townyelections.model.Candidate");
            electionPhaseClass = Class.forName("com.townyelections.model.ElectionPhase");
            townyHookClass = Class.forName("com.townyelections.integration.TownyHook");
            constituencyClass = Class.forName("com.townyelections.integration.Constituency");
            operationResultClass = Class.forName("com.townyelections.model.OperationResult");
            
            townClass = Class.forName("com.palmergames.bukkit.towny.object.Town");
            residentClass = Class.forName("com.palmergames.bukkit.towny.object.Resident");

            // Get electionManager from TownyElections
            Method getElectionManager = townyElectionsClass.getMethod("getElectionManager");
            electionManager = getElectionManager.invoke(townyElections);

            // Get townyHook from TownyElections
            Method getTownyHook = townyElectionsClass.getMethod("getTownyHook");
            townyHook = getTownyHook.invoke(townyElections);

            initialized = true;
            extension.logger().info("TownyElections bridge initialized successfully using reflection");

        } catch (Exception e) {
            extension.logger().log(Level.SEVERE, "Error initializing TownyElections bridge: " + e.getMessage(), e);
        }
    }

    public void shutdown() {
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
    public Object getElectionForTown(UUID townUuid) {
        if (electionManager == null || electionManagerClass == null) {
            return null;
        }
        try {
            Method getElection = electionManagerClass.getMethod("getElection", UUID.class);
            return getElection.invoke(electionManager, townUuid);
        } catch (Exception e) {
            extension.logger().log(Level.WARNING, "Error getting election for town: " + e.getMessage(), e);
            return null;
        }
    }

    /**
     * Get election for a player's town
     */
    public Object getElectionForPlayer(UUID playerId) {
        if (electionManager == null || townyHook == null) {
            return null;
        }

        try {
            // Get resident
            Method getResident = townyHookClass.getMethod("getResident", UUID.class);
            Object resident = getResident.invoke(townyHook, playerId);
            
            if (resident == null) {
                return null;
            }

            // Check if resident has town
            Method hasTown = residentClass.getMethod("hasTown");
            if (!(Boolean) hasTown.invoke(resident)) {
                return null;
            }

            // Get town
            Method getTownOrNull = residentClass.getMethod("getTownOrNull");
            Object town = getTownOrNull.invoke(resident);
            
            if (town == null) {
                return null;
            }

            // Get town UUID
            Method getUUID = townClass.getMethod("getUUID");
            UUID townUuid = (UUID) getUUID.invoke(town);

            // Get election
            Method getElection = electionManagerClass.getMethod("getElection", UUID.class);
            return getElection.invoke(electionManager, townUuid);
        } catch (Exception e) {
            extension.logger().log(Level.WARNING, "Error getting election for player: " + e.getMessage(), e);
            return null;
        }
    }

    /**
     * Get all active elections
     */
    public Collection<?> getActiveElections() {
        if (electionManager == null || electionManagerClass == null) {
            return Collections.emptyList();
        }
        try {
            Method getActiveElections = electionManagerClass.getMethod("getActiveElections");
            Object activeMap = getActiveElections.invoke(electionManager);
            Method values = activeMap.getClass().getMethod("values");
            return (Collection<?>) values.invoke(activeMap);
        } catch (Exception e) {
            extension.logger().log(Level.WARNING, "Error getting active elections: " + e.getMessage(), e);
            return Collections.emptyList();
        }
    }

    /**
     * Check if a player can vote in their town's election
     */
    public boolean canVote(UUID playerId) {
        Object election = getElectionForPlayer(playerId);
        if (election == null) {
            return false;
        }

        try {
            Method getPhase = electionClass.getMethod("getPhase");
            Object phase = getPhase.invoke(election);
            
            // Check if phase is VOTING or RUNOFF
            Method nameMethod = electionPhaseClass.getMethod("name");
            String phaseName = (String) nameMethod.invoke(phase);
            
            return "VOTING".equals(phaseName) || "RUNOFF".equals(phaseName);
        } catch (Exception e) {
            extension.logger().log(Level.WARNING, "Error checking if player can vote: " + e.getMessage(), e);
            return false;
        }
    }

    /**
     * Check if a player has already voted
     */
    public boolean hasVoted(UUID playerId) {
        Object election = getElectionForPlayer(playerId);
        if (election == null) {
            return false;
        }
        try {
            Method hasVoted = electionClass.getMethod("hasVoted", UUID.class);
            return (Boolean) hasVoted.invoke(election, playerId);
        } catch (Exception e) {
            extension.logger().log(Level.WARNING, "Error checking if player has voted: " + e.getMessage(), e);
            return false;
        }
    }

    /**
     * Get all candidates for a player's town election
     */
    public List<Object> getCandidates(UUID playerId) {
        Object election = getElectionForPlayer(playerId);
        if (election == null) {
            return Collections.emptyList();
        }
        try {
            Method getCandidateList = electionClass.getMethod("getCandidateList");
            Object candidateList = getCandidateList.invoke(election);
            return new ArrayList<>((Collection<?>) candidateList);
        } catch (Exception e) {
            extension.logger().log(Level.WARNING, "Error getting candidates: " + e.getMessage(), e);
            return Collections.emptyList();
        }
    }

    /**
     * Get all candidates for a specific town
     */
    public List<Object> getCandidatesForTown(UUID townUuid) {
        Object election = getElectionForTown(townUuid);
        if (election == null) {
            return Collections.emptyList();
        }
        try {
            Method getCandidateList = electionClass.getMethod("getCandidateList");
            Object candidateList = getCandidateList.invoke(election);
            return new ArrayList<>((Collection<?>) candidateList);
        } catch (Exception e) {
            extension.logger().log(Level.WARNING, "Error getting candidates for town: " + e.getMessage(), e);
            return Collections.emptyList();
        }
    }

    /**
     * Cast a vote for a Bedrock player
     */
    public boolean castVote(UUID playerId, UUID candidateId) {
        if (electionManager == null || townyHook == null) {
            return false;
        }

        try {
            // Get resident
            Method getResident = townyHookClass.getMethod("getResident", UUID.class);
            Object resident = getResident.invoke(townyHook, playerId);
            
            Player bukkitPlayer = Bukkit.getPlayer(playerId);
            
            if (resident == null || bukkitPlayer == null) {
                return false;
            }

            // Get town
            Method getPlayerTown = townyHookClass.getMethod("getPlayerTown", Player.class);
            Object town = getPlayerTown.invoke(townyHook, bukkitPlayer);
            
            if (town == null) {
                return false;
            }

            // Create constituency
            Method ofMethod = townyHookClass.getMethod("of", townClass);
            Object constituency = ofMethod.invoke(townyHook, town);
            
            if (constituency == null) {
                return false;
            }

            // Cast vote
            Method castVote = electionManagerClass.getMethod("castVote", 
                residentClass, townClass, UUID.class, Player.class);
            Object result = castVote.invoke(electionManager, resident, town, candidateId, bukkitPlayer);
            
            if (result == null) {
                return false;
            }

            Method success = operationResultClass.getMethod("success");
            return (Boolean) success.invoke(result);
        } catch (Exception e) {
            extension.logger().log(Level.WARNING, "Error casting vote for Bedrock player: " + e.getMessage(), e);
            return false;
        }
    }

    /**
     * Get election phase for a player's town
     */
    public Object getElectionPhase(UUID playerId) {
        Object election = getElectionForPlayer(playerId);
        if (election == null) {
            return null;
        }
        try {
            Method getPhase = electionClass.getMethod("getPhase");
            return getPhase.invoke(election);
        } catch (Exception e) {
            extension.logger().log(Level.WARNING, "Error getting election phase: " + e.getMessage(), e);
            return null;
        }
    }

    /**
     * Get time remaining for current election phase
     */
    public long getTimeRemaining(UUID playerId) {
        Object election = getElectionForPlayer(playerId);
        if (election == null) {
            return 0;
        }
        try {
            Method getMillisRemaining = electionClass.getMethod("getMillisRemaining");
            return (Long) getMillisRemaining.invoke(election);
        } catch (Exception e) {
            extension.logger().log(Level.WARNING, "Error getting time remaining: " + e.getMessage(), e);
            return 0;
        }
    }

    /**
     * Get town name for a player
     */
    public String getTownName(UUID playerId) {
        if (townyHook == null || townyHookClass == null) {
            return "Unknown";
        }

        try {
            Method getResident = townyHookClass.getMethod("getResident", UUID.class);
            Object resident = getResident.invoke(townyHook, playerId);
            
            if (resident == null) {
                return "Unknown";
            }

            Method hasTown = residentClass.getMethod("hasTown");
            if (!(Boolean) hasTown.invoke(resident)) {
                return "Unknown";
            }

            Method getTownOrNull = residentClass.getMethod("getTownOrNull");
            Object town = getTownOrNull.invoke(resident);
            
            if (town == null) {
                return "Unknown";
            }

            Method getName = townClass.getMethod("getName");
            return (String) getName.invoke(town);
        } catch (Exception e) {
            extension.logger().log(Level.WARNING, "Error getting town name: " + e.getMessage(), e);
            return "Unknown";
        }
    }

    /**
     * Get candidate by UUID from any active election
     */
    public Object getCandidate(UUID candidateId) {
        if (electionManager == null || electionManagerClass == null) {
            return null;
        }

        try {
            Method getActiveElections = electionManagerClass.getMethod("getActiveElections");
            Object activeMap = getActiveElections.invoke(electionManager);
            Method values = activeMap.getClass().getMethod("values");
            Collection<?> elections = (Collection<?>) values.invoke(activeMap);
            
            Method getCandidate = electionClass.getMethod("getCandidate", UUID.class);
            
            for (Object election : elections) {
                Object candidate = getCandidate.invoke(election, candidateId);
                if (candidate != null) {
                    return candidate;
                }
            }
            return null;
        } catch (Exception e) {
            extension.logger().log(Level.WARNING, "Error getting candidate: " + e.getMessage(), e);
            return null;
        }
    }

    /**
     * Get total votes cast in election
     */
    public int getTotalVotes(UUID playerId) {
        Object election = getElectionForPlayer(playerId);
        if (election == null) {
            return 0;
        }
        try {
            Method getTotalVotes = electionClass.getMethod("getTotalVotes");
            return (Integer) getTotalVotes.invoke(election);
        } catch (Exception e) {
            extension.logger().log(Level.WARNING, "Error getting total votes: " + e.getMessage(), e);
            return 0;
        }
    }

    /**
     * Get the vote tally for an election
     */
    public Map<UUID, Integer> getVoteTally(UUID playerId) {
        Object election = getElectionForPlayer(playerId);
        if (election == null) {
            return Collections.emptyMap();
        }
        try {
            Method tally = electionClass.getMethod("tally");
            return (Map<UUID, Integer>) tally.invoke(election);
        } catch (Exception e) {
            extension.logger().log(Level.WARNING, "Error getting vote tally: " + e.getMessage(), e);
            return Collections.emptyMap();
        }
    }

    /**
     * Get the ballot for a specific player
     */
    public List<UUID> getBallot(UUID playerId) {
        Object election = getElectionForPlayer(playerId);
        if (election == null) {
            return Collections.emptyList();
        }
        try {
            Method getBallot = electionClass.getMethod("getBallot", UUID.class);
            return (List<UUID>) getBallot.invoke(election, playerId);
        } catch (Exception e) {
            extension.logger().log(Level.WARNING, "Error getting ballot: " + e.getMessage(), e);
            return Collections.emptyList();
        }
    }

    // Helper methods for Candidate objects
    public UUID getCandidateId(Object candidate) {
        if (candidate == null || candidateClass == null) {
            return null;
        }
        try {
            Method getId = candidateClass.getMethod("getId");
            return (UUID) getId.invoke(candidate);
        } catch (Exception e) {
            return null;
        }
    }

    public String getCandidateName(Object candidate) {
        if (candidate == null || candidateClass == null) {
            return "Unknown";
        }
        try {
            Method getName = candidateClass.getMethod("getName");
            return (String) getName.invoke(candidate);
        } catch (Exception e) {
            return "Unknown";
        }
    }

    public String getCandidateParty(Object candidate) {
        if (candidate == null || candidateClass == null) {
            return null;
        }
        try {
            Method getParty = candidateClass.getMethod("getParty");
            Object party = getParty.invoke(candidate);
            if (party != null) {
                Method getName = party.getClass().getMethod("getName");
                return (String) getName.invoke(party);
            }
            return null;
        } catch (Exception e) {
            return null;
        }
    }

    public boolean isInitialized() {
        return initialized;
    }
}
