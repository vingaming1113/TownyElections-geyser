package com.townyelections.geyser;

import java.util.*;
import java.lang.reflect.Method;
import java.lang.reflect.Field;
import java.util.logging.Level;

/**
 * Bridge class that connects TownyElections functionality to GeyserMC using reflection.
 * This avoids ALL compile-time dependencies - everything is loaded via reflection.
 */
public class TownyElectionsBridge {

    private final TownyElectionsExtension extension;
    private Object geyserAPI;
    
    // Reflection cached references for Geyser API
    private Class<?> geyserAPIClass;
    private Class<?> geyserPlayerClass;
    private Class<?> eventBusClass;
    private Class<?> playerJoinEventClass;
    private Class<?> playerQuitEventClass;
    private Class<?> subscribeAnnotationClass;
    private Class<?> customFormClass;
    private Class<?> simpleFormClass;
    private Class<?> formResponseClass;
    private Class<?> formBuilderClass;
    private Class<?> customFormBuilderClass;
    private Class<?> simpleFormBuilderClass;
    
    // Reflection cached references for TownyElections
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
    
    // Reflection cached references for Bukkit
    private Class<?> bukkitClass;
    private Class<?> pluginClass;
    private Class<?> playerClass;
    private Class<?> pluginManagerClass;
    private Class<?> schedulerClass;
    
    private Object townyElections;
    private Object electionManager;
    private Object townyHook;
    private Object bukkit;
    private boolean initialized = false;

    public TownyElectionsBridge(TownyElectionsExtension extension, Object geyserAPI) {
        this.extension = extension;
        this.geyserAPI = geyserAPI;
    }

    @SuppressWarnings("unchecked")
    public void initialize() {
        if (initialized) {
            return;
        }

        try {
            // Load Geyser API classes
            geyserAPIClass = Class.forName("org.geysermc.api.GeyserAPI");
            geyserPlayerClass = Class.forName("org.geysermc.api.connection.player.GeyserPlayer");
            eventBusClass = Class.forName("org.geysermc.api.event.bus.EventBus");
            playerJoinEventClass = Class.forName("org.geysermc.api.event.player.PlayerJoinEvent");
            playerQuitEventClass = Class.forName("org.geysermc.api.event.player.PlayerQuitEvent");
            subscribeAnnotationClass = Class.forName("org.geysermc.api.event.Subscribe");
            customFormClass = Class.forName("org.geysermc.api.form.CustomForm");
            simpleFormClass = Class.forName("org.geysermc.api.form.SimpleForm");
            formResponseClass = Class.forName("org.geysermc.api.form.FormResponse");
            customFormBuilderClass = Class.forName("org.geysermc.api.form.CustomForm$Builder");
            simpleFormBuilderClass = Class.forName("org.geysermc.api.form.SimpleForm$Builder");
            
            // Load Bukkit classes
            bukkitClass = Class.forName("org.bukkit.Bukkit");
            pluginClass = Class.forName("org.bukkit.plugin.Plugin");
            playerClass = Class.forName("org.bukkit.entity.Player");
            pluginManagerClass = Class.forName("org.bukkit.plugin.PluginManager");
            schedulerClass = Class.forName("org.bukkit.scheduler.BukkitScheduler");
            
            // Load TownyElections classes
            townyElectionsClass = Class.forName("com.townyelections.TownyElections");
            electionManagerClass = Class.forName("com.townyelections.manager.ElectionManager");
            electionClass = Class.forName("com.townyelections.model.Election");
            candidateClass = Class.forName("com.townyelections.model.Candidate");
            electionPhaseClass = Class.forName("com.townyelections.model.ElectionPhase");
            townyHookClass = Class.forName("com.townyelections.integration.TownyHook");
            constituencyClass = Class.forName("com.townyelections.integration.Constituency");
            operationResultClass = Class.forName("com.townyelections.model.OperationResult");
            
            townClass = Class.forName("com.palmergames.bukkit.towny.object.Town");
            residentClass = Class.forName("com.palmergames.bukkit.towny.object.Resident");

            // Get TownyElections plugin via Bukkit
            Method getPluginManager = bukkitClass.getMethod("getPluginManager");
            Object pluginManager = getPluginManager.invoke(null);
            Method getPlugin = pluginManagerClass.getMethod("getPlugin", String.class);
            Object plugin = getPlugin.invoke(pluginManager, "TownyElections");
            
            if (plugin == null) {
                extension.log(Level.SEVERE, "TownyElections plugin not found");
                return;
            }
            townyElections = plugin;

            // Get electionManager from TownyElections
            Method getElectionManager = townyElectionsClass.getMethod("getElectionManager");
            electionManager = getElectionManager.invoke(townyElections);

            // Get townyHook from TownyElections
            Method getTownyHook = townyElectionsClass.getMethod("getTownyHook");
            townyHook = getTownyHook.invoke(townyElections);

            // Cache Bukkit instance
            bukkit = null; // Bukkit is static, accessed via class methods

            initialized = true;
            extension.log(Level.INFO, "TownyElections bridge initialized successfully using reflection");

        } catch (Exception e) {
            extension.log(Level.SEVERE, "Error initializing TownyElections bridge: " + e.getMessage(), e);
        }
    }

    public void shutdown() {
        initialized = false;
        extension.log(Level.INFO, "TownyElections bridge shutdown complete");
    }

    public boolean isBedrockPlayer(UUID playerId) {
        try {
            Method isBedrockPlayer = geyserAPIClass.getMethod("isBedrockPlayer", UUID.class);
            return (Boolean) isBedrockPlayer.invoke(geyserAPI, playerId);
        } catch (Exception e) {
            extension.log(Level.WARNING, "Error checking if player is Bedrock: " + e.getMessage(), e);
            return false;
        }
    }

    public Object getGeyserPlayer(UUID playerId) {
        try {
            Method getConnection = geyserAPIClass.getMethod("getConnection", UUID.class);
            return getConnection.invoke(geyserAPI, playerId);
        } catch (Exception e) {
            extension.log(Level.WARNING, "Error getting GeyserPlayer: " + e.getMessage(), e);
            return null;
        }
    }

    public Object getElectionForTown(UUID townUuid) {
        if (electionManager == null || electionManagerClass == null) {
            return null;
        }
        try {
            Method getElection = electionManagerClass.getMethod("getElection", UUID.class);
            return getElection.invoke(electionManager, townUuid);
        } catch (Exception e) {
            extension.log(Level.WARNING, "Error getting election for town: " + e.getMessage(), e);
            return null;
        }
    }

    public Object getElectionForPlayer(UUID playerId) {
        if (electionManager == null || townyHook == null) {
            return null;
        }

        try {
            Method getResident = townyHookClass.getMethod("getResident", UUID.class);
            Object resident = getResident.invoke(townyHook, playerId);
            
            if (resident == null) {
                return null;
            }

            Method hasTown = residentClass.getMethod("hasTown");
            if (!(Boolean) hasTown.invoke(resident)) {
                return null;
            }

            Method getTownOrNull = residentClass.getMethod("getTownOrNull");
            Object town = getTownOrNull.invoke(resident);
            
            if (town == null) {
                return null;
            }

            Method getUUID = townClass.getMethod("getUUID");
            UUID townUuid = (UUID) getUUID.invoke(town);

            Method getElection = electionManagerClass.getMethod("getElection", UUID.class);
            return getElection.invoke(electionManager, townUuid);
        } catch (Exception e) {
            extension.log(Level.WARNING, "Error getting election for player: " + e.getMessage(), e);
            return null;
        }
    }

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
            extension.log(Level.WARNING, "Error getting active elections: " + e.getMessage(), e);
            return Collections.emptyList();
        }
    }

    public boolean canVote(UUID playerId) {
        Object election = getElectionForPlayer(playerId);
        if (election == null) {
            return false;
        }

        try {
            Method getPhase = electionClass.getMethod("getPhase");
            Object phase = getPhase.invoke(election);
            
            Method nameMethod = electionPhaseClass.getMethod("name");
            String phaseName = (String) nameMethod.invoke(phase);
            
            return "VOTING".equals(phaseName) || "RUNOFF".equals(phaseName);
        } catch (Exception e) {
            extension.log(Level.WARNING, "Error checking if player can vote: " + e.getMessage(), e);
            return false;
        }
    }

    public boolean hasVoted(UUID playerId) {
        Object election = getElectionForPlayer(playerId);
        if (election == null) {
            return false;
        }
        try {
            Method hasVoted = electionClass.getMethod("hasVoted", UUID.class);
            return (Boolean) hasVoted.invoke(election, playerId);
        } catch (Exception e) {
            extension.log(Level.WARNING, "Error checking if player has voted: " + e.getMessage(), e);
            return false;
        }
    }

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
            extension.log(Level.WARNING, "Error getting candidates: " + e.getMessage(), e);
            return Collections.emptyList();
        }
    }

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
            extension.log(Level.WARNING, "Error getting candidates for town: " + e.getMessage(), e);
            return Collections.emptyList();
        }
    }

    public boolean castVote(UUID playerId, UUID candidateId) {
        if (electionManager == null || townyHook == null) {
            return false;
        }

        try {
            Method getResident = townyHookClass.getMethod("getResident", UUID.class);
            Object resident = getResident.invoke(townyHook, playerId);
            
            Method getPlayer = bukkitClass.getMethod("getPlayer", UUID.class);
            Object bukkitPlayer = getPlayer.invoke(null, playerId);
            
            if (resident == null || bukkitPlayer == null) {
                return false;
            }

            Method getPlayerTown = townyHookClass.getMethod("getPlayerTown", playerClass);
            Object town = getPlayerTown.invoke(townyHook, bukkitPlayer);
            
            if (town == null) {
                return false;
            }

            Method ofMethod = townyHookClass.getMethod("of", townClass);
            Object constituency = ofMethod.invoke(townyHook, town);
            
            if (constituency == null) {
                return false;
            }

            Method castVote = electionManagerClass.getMethod("castVote", 
                residentClass, townClass, UUID.class, playerClass);
            Object result = castVote.invoke(electionManager, resident, town, candidateId, bukkitPlayer);
            
            if (result == null) {
                return false;
            }

            Method success = operationResultClass.getMethod("success");
            return (Boolean) success.invoke(result);
        } catch (Exception e) {
            extension.log(Level.WARNING, "Error casting vote for Bedrock player: " + e.getMessage(), e);
            return false;
        }
    }

    public Object getElectionPhase(UUID playerId) {
        Object election = getElectionForPlayer(playerId);
        if (election == null) {
            return null;
        }
        try {
            Method getPhase = electionClass.getMethod("getPhase");
            return getPhase.invoke(election);
        } catch (Exception e) {
            extension.log(Level.WARNING, "Error getting election phase: " + e.getMessage(), e);
            return null;
        }
    }

    public long getTimeRemaining(UUID playerId) {
        Object election = getElectionForPlayer(playerId);
        if (election == null) {
            return 0;
        }
        try {
            Method getMillisRemaining = electionClass.getMethod("getMillisRemaining");
            return (Long) getMillisRemaining.invoke(election);
        } catch (Exception e) {
            extension.log(Level.WARNING, "Error getting time remaining: " + e.getMessage(), e);
            return 0;
        }
    }

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
            extension.log(Level.WARNING, "Error getting town name: " + e.getMessage(), e);
            return "Unknown";
        }
    }

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
            extension.log(Level.WARNING, "Error getting candidate: " + e.getMessage(), e);
            return null;
        }
    }

    public int getTotalVotes(UUID playerId) {
        Object election = getElectionForPlayer(playerId);
        if (election == null) {
            return 0;
        }
        try {
            Method getTotalVotes = electionClass.getMethod("getTotalVotes");
            return (Integer) getTotalVotes.invoke(election);
        } catch (Exception e) {
            extension.log(Level.WARNING, "Error getting total votes: " + e.getMessage(), e);
            return 0;
        }
    }

    public Map<UUID, Integer> getVoteTally(UUID playerId) {
        Object election = getElectionForPlayer(playerId);
        if (election == null) {
            return Collections.emptyMap();
        }
        try {
            Method tally = electionClass.getMethod("tally");
            return (Map<UUID, Integer>) tally.invoke(election);
        } catch (Exception e) {
            extension.log(Level.WARNING, "Error getting vote tally: " + e.getMessage(), e);
            return Collections.emptyMap();
        }
    }

    public List<UUID> getBallot(UUID playerId) {
        Object election = getElectionForPlayer(playerId);
        if (election == null) {
            return Collections.emptyList();
        }
        try {
            Method getBallot = electionClass.getMethod("getBallot", UUID.class);
            return (List<UUID>) getBallot.invoke(election, playerId);
        } catch (Exception e) {
            extension.log(Level.WARNING, "Error getting ballot: " + e.getMessage(), e);
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

    // Helper methods for sending forms
    public void sendForm(Object geyserPlayer, Object form) {
        if (geyserPlayer == null) {
            return;
        }
        try {
            Method sendForm = geyserPlayerClass.getMethod("sendForm", Object.class);
            sendForm.invoke(geyserPlayer, form);
        } catch (Exception e) {
            extension.log(Level.WARNING, "Error sending form: " + e.getMessage(), e);
        }
    }

    public Object createCustomForm() {
        try {
            Method builder = customFormClass.getMethod("builder");
            return builder.invoke(null);
        } catch (Exception e) {
            extension.log(Level.WARNING, "Error creating CustomForm builder: " + e.getMessage(), e);
            return null;
        }
    }

    public Object createSimpleForm() {
        try {
            Method builder = simpleFormClass.getMethod("builder");
            return builder.invoke(null);
        } catch (Exception e) {
            extension.log(Level.WARNING, "Error creating SimpleForm builder: " + e.getMessage(), e);
            return null;
        }
    }

    public void setFormTitle(Object formBuilder, String title) {
        try {
            Method titleMethod = formBuilder.getClass().getMethod("title", String.class);
            titleMethod.invoke(formBuilder, title);
        } catch (Exception e) {
            extension.log(Level.WARNING, "Error setting form title: " + e.getMessage(), e);
        }
    }

    public void addFormText(Object formBuilder, String text) {
        try {
            Method textMethod = formBuilder.getClass().getMethod("text", String.class);
            textMethod.invoke(formBuilder, text);
        } catch (Exception e) {
            extension.log(Level.WARNING, "Error adding form text: " + e.getMessage(), e);
        }
    }

    public void addFormButton(Object formBuilder, String text, String value) {
        try {
            Method buttonMethod = formBuilder.getClass().getMethod("button", String.class, String.class);
            buttonMethod.invoke(formBuilder, text, value);
        } catch (Exception e) {
            extension.log(Level.WARNING, "Error adding form button: " + e.getMessage(), e);
        }
    }

    public Object buildForm(Object formBuilder) {
        try {
            Method buildMethod = formBuilder.getClass().getMethod("build");
            return buildMethod.invoke(formBuilder);
        } catch (Exception e) {
            extension.log(Level.WARNING, "Error building form: " + e.getMessage(), e);
            return null;
        }
    }

    public void setFormResponder(Object formBuilder, Object handler) {
        try {
            Method respondWith = formBuilder.getClass().getMethod("respondWith", Class.forName("org.geysermc.api.form.util.FormHandler"));
            respondWith.invoke(formBuilder, handler);
        } catch (Exception e) {
            // Form responder is optional
        }
    }

    public Object getEventBus() {
        try {
            Method eventBus = geyserAPIClass.getMethod("eventBus");
            return eventBus.invoke(geyserAPI);
        } catch (Exception e) {
            extension.log(Level.WARNING, "Error getting event bus: " + e.getMessage(), e);
            return null;
        }
    }

    public void registerEventListener(Object eventBus, Object listener) {
        try {
            Method subscribe = eventBusClass.getMethod("subscribe", Object.class);
            subscribe.invoke(eventBus, listener);
        } catch (Exception e) {
            extension.log(Level.WARNING, "Error subscribing to events: " + e.getMessage(), e);
        }
    }

    public void runTaskLater(Object plugin, Runnable task, long delay) {
        try {
            Method getScheduler = bukkitClass.getMethod("getScheduler");
            Object scheduler = getScheduler.invoke(null);
            Method runTaskLater = schedulerClass.getMethod("runTaskLater", pluginClass, Runnable.class, long.class);
            runTaskLater.invoke(scheduler, plugin, task, delay);
        } catch (Exception e) {
            extension.log(Level.WARNING, "Error scheduling task: " + e.getMessage(), e);
        }
    }

    public Object getTownyElectionsPlugin() {
        return townyElections;
    }

    public boolean isInitialized() {
        return initialized;
    }
}
