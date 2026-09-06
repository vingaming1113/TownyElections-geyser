package com.townyelections.geyser;

import java.util.UUID;
import java.util.List;
import java.util.HashMap;
import java.util.Map;
import java.lang.reflect.Method;
import java.lang.reflect.Field;
import java.util.logging.Level;
import java.util.Collection;
import java.util.ArrayList;

/**
 * Manages Bedrock Forms GUI for TownyElections.
 * Creates and handles native Bedrock forms for all election interactions.
 * 
 * This is the main feature of the extension - providing Bedrock players with
 * native Forms GUI instead of the Java inventory GUI.
 * 
 * Uses reflection to avoid compile-time dependencies.
 */
public class ElectionFormsManager {

    private final TownyElectionsExtension extension;
    private final Object geyserAPI;
    private final TownyElectionsBridge bridge;
    private final Map<UUID, Long> lastFormSent = new HashMap<>();
    private boolean initialized = false;
    
    // Reflection cached references
    private Class<?> geyserPlayerClass;
    private Class<?> eventBusClass;
    private Class<?> playerJoinEventClass;
    private Class<?> playerQuitEventClass;
    private Class<?> customFormClass;
    private Class<?> simpleFormClass;
    private Class<?> formResponseClass;
    private Class<?> formBuilderClass;
    private Class<?> customFormBuilderClass;
    private Class<?> simpleFormBuilderClass;
    private Class<?> bukkitClass;
    private Class<?> schedulerClass;

    public ElectionFormsManager(TownyElectionsExtension extension, Object geyserAPI, TownyElectionsBridge bridge) {
        this.extension = extension;
        this.geyserAPI = geyserAPI;
        this.bridge = bridge;
    }

    public void initialize() {
        if (initialized) {
            return;
        }

        try {
            // Load classes
            geyserPlayerClass = Class.forName("org.geysermc.api.connection.player.GeyserPlayer");
            eventBusClass = Class.forName("org.geysermc.api.event.bus.EventBus");
            playerJoinEventClass = Class.forName("org.geysermc.api.event.player.PlayerJoinEvent");
            playerQuitEventClass = Class.forName("org.geysermc.api.event.player.PlayerQuitEvent");
            customFormClass = Class.forName("org.geysermc.api.form.CustomForm");
            simpleFormClass = Class.forName("org.geysermc.api.form.SimpleForm");
            formResponseClass = Class.forName("org.geysermc.api.form.FormResponse");
            bukkitClass = Class.forName("org.bukkit.Bukkit");
            schedulerClass = Class.forName("org.bukkit.scheduler.BukkitScheduler");
            
            // Get event bus and subscribe
            Method eventBusMethod = geyserAPI.getClass().getMethod("eventBus");
            Object eventBus = eventBusMethod.invoke(geyserAPI);
            Method subscribe = eventBusClass.getMethod("subscribe", Object.class);
            subscribe.invoke(eventBus, this);

            initialized = true;
            extension.log(Level.INFO, "ElectionFormsManager initialized - Bedrock Forms GUI enabled");
        } catch (Exception e) {
            extension.log(Level.SEVERE, "Error initializing ElectionFormsManager: " + e.getMessage(), e);
        }
    }

    public void shutdown() {
        if (!initialized) {
            return;
        }

        initialized = false;
        lastFormSent.clear();
        extension.log(Level.INFO, "ElectionFormsManager shutdown");
    }

    public void onPlayerJoin(Object event) {
        try {
            Method getPlayer = playerJoinEventClass.getMethod("getPlayer");
            Object geyserPlayerObj = getPlayer.invoke(event);
            Method getUuid = geyserPlayerClass.getMethod("getUuid");
            UUID playerId = (UUID) getUuid.invoke(geyserPlayerObj);
            
            // Only handle Bedrock players
            if (!bridge.isBedrockPlayer(playerId)) {
                return;
            }

            Method getUsername = geyserPlayerClass.getMethod("getUsername");
            String username = (String) getUsername.invoke(geyserPlayerObj);
            extension.log(Level.INFO, "Bedrock player joined: " + username + 
                          " - Showing election form");
            
            // Send main election form after a short delay
            Object plugin = extension.getTownyElectionsPlugin();
            bridge.runTaskLater(plugin, () -> {
                showMainElectionForm(playerId);
            }, 20L); // 1 second delay
        } catch (Exception e) {
            extension.log(Level.WARNING, "Error handling player join: " + e.getMessage(), e);
        }
    }

    public void onPlayerQuit(Object event) {
        try {
            Method getPlayer = playerQuitEventClass.getMethod("getPlayer");
            Object geyserPlayerObj = getPlayer.invoke(event);
            Method getUuid = geyserPlayerClass.getMethod("getUuid");
            UUID playerId = (UUID) getUuid.invoke(geyserPlayerObj);
            lastFormSent.remove(playerId);
        } catch (Exception e) {
            extension.log(Level.WARNING, "Error handling player quit: " + e.getMessage(), e);
        }
    }

    /**
     * Show the main election form to a Bedrock player
     * This automatically detects the election phase and shows the appropriate form
     */
    public void showMainElectionForm(UUID playerId) {
        if (!bridge.isInitialized()) {
            extension.log(Level.WARNING, "Bridge not initialized, cannot show form");
            return;
        }

        Object election = bridge.getElectionForPlayer(playerId);
        
        if (election == null) {
            showNoElectionForm(playerId);
            return;
        }

        Object phase = bridge.getElectionPhase(playerId);
        
        if (phase == null) {
            showNoElectionForm(playerId);
            return;
        }

        // Use reflection to get phase name
        try {
            Method nameMethod = phase.getClass().getMethod("name");
            String phaseName = (String) nameMethod.invoke(phase);
            
            switch (phaseName) {
                case "NOMINATION":
                    showNominationForm(playerId, election);
                    break;
                case "VOTING":
                case "RUNOFF":
                    showVotingForm(playerId, election);
                    break;
                case "CONCLUDED":
                    showResultsForm(playerId, election);
                    break;
                default:
                    showNoElectionForm(playerId);
            }
        } catch (Exception e) {
            extension.log(Level.WARNING, "Error getting phase name: " + e.getMessage(), e);
            showNoElectionForm(playerId);
        }
    }

    /**
     * Show form when there's no active election in the player's town
     */
    private void showNoElectionForm(UUID playerId) {
        String townName = bridge.getTownName(playerId);
        
        Object geyserPlayer = bridge.getGeyserPlayer(playerId);
        if (geyserPlayer == null) {
            extension.log(Level.WARNING, "Cannot get GeyserPlayer for " + playerId);
            return;
        }

        Object formBuilder = bridge.createCustomForm();
        bridge.setFormTitle(formBuilder, "Town Elections");
        bridge.addFormText(formBuilder, "\u00a76No active election in " + townName);
        bridge.addFormText(formBuilder, "\u00a77Elections will appear here when they start.");
        bridge.addFormButton(formBuilder, "\u00a7aOK", "close");
        Object form = bridge.buildForm(formBuilder);
        bridge.sendForm(geyserPlayer, form);
    }

    /**
     * Show nomination phase form - displays current candidates
     */
    private void showNominationForm(UUID playerId, Object election) {
        String townName = bridge.getTownName(playerId);
        long timeRemaining = bridge.getTimeRemaining(playerId);
        String timeStr = formatTime(timeRemaining);

        Object geyserPlayer = bridge.getGeyserPlayer(playerId);
        if (geyserPlayer == null) {
            return;
        }

        Object formBuilder = bridge.createCustomForm();
        bridge.setFormTitle(formBuilder, "\u00a76Town Election - " + townName);
        bridge.addFormText(formBuilder, "\u00a7eNomination Phase");
        bridge.addFormText(formBuilder, "\u00a77Time remaining: " + timeStr);
        
        List<Object> candidates = bridge.getCandidates(playerId);
        if (candidates.isEmpty()) {
            bridge.addFormText(formBuilder, "\u00a7cNo candidates have registered yet.");
        } else {
            bridge.addFormText(formBuilder, "\u00a7aCurrent Candidates:");
            for (Object candidate : candidates) {
                String candidateName = bridge.getCandidateName(candidate);
                bridge.addFormText(formBuilder, "\u00a77- " + candidateName);
            }
        }
        
        bridge.addFormButton(formBuilder, "\u00a7aRefresh", "refresh");
        bridge.addFormButton(formBuilder, "\u00a7cClose", "close");
        Object form = bridge.buildForm(formBuilder);
        bridge.sendForm(geyserPlayer, form);
    }

    /**
     * Show voting form with candidate selection buttons
     */
    private void showVotingForm(UUID playerId, Object election) {
        String townName = bridge.getTownName(playerId);
        long timeRemaining = bridge.getTimeRemaining(playerId);
        String timeStr = formatTime(timeRemaining);
        boolean hasVoted = bridge.hasVoted(playerId);

        Object geyserPlayer = bridge.getGeyserPlayer(playerId);
        if (geyserPlayer == null) {
            return;
        }

        if (hasVoted) {
            showAlreadyVotedForm(playerId, election);
            return;
        }

        List<Object> candidates = bridge.getCandidates(playerId);
        
        if (candidates.isEmpty()) {
            showNoCandidatesForm(playerId);
            return;
        }

        Object formBuilder = bridge.createSimpleForm();
        bridge.setFormTitle(formBuilder, "\u00a76Vote in " + townName + " Election");
        bridge.addFormText(formBuilder, "\u00a77Time remaining: " + timeStr);
        bridge.addFormText(formBuilder, "\u00a7aSelect a candidate to vote for:");
        
        for (Object candidate : candidates) {
            UUID candidateId = bridge.getCandidateId(candidate);
            String party = bridge.getCandidateParty(candidate);
            String displayName = bridge.getCandidateName(candidate);
            if (candidateId == null) {
                continue;
            }
            if (party != null && !party.equals("Independent") && !party.isEmpty()) {
                displayName = "\u00a7e[" + party + "] \u00a7f" + displayName;
            }
            bridge.addFormButton(formBuilder, displayName, "vote_" + candidateId.toString());
        }
        
        bridge.addFormButton(formBuilder, "\u00a7cCancel", "close");
        Object form = bridge.buildForm(formBuilder);
        bridge.sendForm(geyserPlayer, form);
    }

    /**
     * Show form when player has already voted
     */
    private void showAlreadyVotedForm(UUID playerId, Object election) {
        UUID votedCandidateId = null;
        List<UUID> ballot = bridge.getBallot(playerId);
        if (!ballot.isEmpty()) {
            votedCandidateId = ballot.get(0);
        }

        Object geyserPlayer = bridge.getGeyserPlayer(playerId);
        if (geyserPlayer == null) {
            return;
        }

        Object formBuilder = bridge.createCustomForm();
        bridge.setFormTitle(formBuilder, "\u00a7aVote Cast!");
        
        if (votedCandidateId != null) {
            String candidateName = bridge.getCandidateName(bridge.getCandidate(votedCandidateId));
            bridge.addFormText(formBuilder, "\u00a7aYou have already voted for:");
            bridge.addFormText(formBuilder, "\u00a77\u00a7l" + candidateName + "\u00a7r");
        } else {
            bridge.addFormText(formBuilder, "\u00a7aYou have already voted.");
        }
        
        bridge.addFormButton(formBuilder, "\u00a7aOK", "close");
        Object form = bridge.buildForm(formBuilder);
        bridge.sendForm(geyserPlayer, form);
    }

    /**
     * Show vote confirmation form after successful vote
     */
    private void showVoteConfirmationForm(UUID playerId, UUID candidateId) {
        String candidateName = bridge.getCandidateName(bridge.getCandidate(candidateId));
        if (candidateName == null || candidateName.equals("Unknown")) {
            candidateName = "the candidate";
        }

        Object geyserPlayer = bridge.getGeyserPlayer(playerId);
        if (geyserPlayer == null) {
            return;
        }

        Object formBuilder = bridge.createCustomForm();
        bridge.setFormTitle(formBuilder, "\u00a7aVote Cast Successfully!");
        bridge.addFormText(formBuilder, "\u00a77You voted for: \u00a7e" + candidateName);
        bridge.addFormText(formBuilder, "\u00a7aThank you for participating!");
        bridge.addFormButton(formBuilder, "\u00a7aOK", "close");
        Object form = bridge.buildForm(formBuilder);
        bridge.sendForm(geyserPlayer, form);
    }

    /**
     * Show vote failed form
     */
    private void showVoteFailedForm(UUID playerId) {
        Object geyserPlayer = bridge.getGeyserPlayer(playerId);
        if (geyserPlayer == null) {
            return;
        }

        Object formBuilder = bridge.createCustomForm();
        bridge.setFormTitle(formBuilder, "\u00a7cVote Failed");
        bridge.addFormText(formBuilder, "\u00a7cCould not cast your vote.");
        bridge.addFormText(formBuilder, "\u00a77You may have already voted or are not eligible.");
        bridge.addFormButton(formBuilder, "\u00a7aOK", "close");
        Object form = bridge.buildForm(formBuilder);
        bridge.sendForm(geyserPlayer, form);
    }

    /**
     * Show form when there are no candidates to vote for
     */
    private void showNoCandidatesForm(UUID playerId) {
        Object geyserPlayer = bridge.getGeyserPlayer(playerId);
        if (geyserPlayer == null) {
            return;
        }

        Object formBuilder = bridge.createCustomForm();
        bridge.setFormTitle(formBuilder, "\u00a7cNo Candidates");
        bridge.addFormText(formBuilder, "\u00a7cThere are no candidates in this election.");
        bridge.addFormText(formBuilder, "\u00a77Check back later when candidates register.");
        bridge.addFormButton(formBuilder, "\u00a7aOK", "close");
        Object form = bridge.buildForm(formBuilder);
        bridge.sendForm(geyserPlayer, form);
    }

    /**
     * Show election results form when election has concluded
     */
    private void showResultsForm(UUID playerId, Object election) {
        String townName = bridge.getTownName(playerId);
        int totalVotes = bridge.getTotalVotes(playerId);

        Object geyserPlayer = bridge.getGeyserPlayer(playerId);
        if (geyserPlayer == null) {
            return;
        }

        Object formBuilder = bridge.createCustomForm();
        bridge.setFormTitle(formBuilder, "\u00a76Election Results - " + townName);
        bridge.addFormText(formBuilder, "\u00a7aElection has concluded!");
        bridge.addFormText(formBuilder, "\u00a77Total votes cast: " + totalVotes);
        
        List<Object> candidates = bridge.getCandidates(playerId);
        if (!candidates.isEmpty()) {
            bridge.addFormText(formBuilder, "\u00a76Results:");
            Map<UUID, Integer> tally = bridge.getVoteTally(playerId);
            for (Object candidate : candidates) {
                UUID candidateId = bridge.getCandidateId(candidate);
                String candidateName = bridge.getCandidateName(candidate);
                int votes = tally.getOrDefault(candidateId, 0);
                bridge.addFormText(formBuilder, "\u00a77" + candidateName + ": " + votes + " votes");
            }
        }
        
        bridge.addFormButton(formBuilder, "\u00a7aOK", "close");
        Object form = bridge.buildForm(formBuilder);
        bridge.sendForm(geyserPlayer, form);
    }

    /**
     * Format time in milliseconds to a human-readable string
     */
    private String formatTime(long millis) {
        if (millis <= 0) {
            return "0s";
        }
        
        long seconds = millis / 1000;
        long minutes = seconds / 60;
        long hours = minutes / 60;
        long days = hours / 24;
        
        if (days > 0) {
            return days + "d " + (hours % 24) + "h " + (minutes % 60) + "m";
        } else if (hours > 0) {
            return hours + "h " + (minutes % 60) + "m";
        } else if (minutes > 0) {
            return minutes + "m " + (seconds % 60) + "s";
        } else {
            return seconds + "s";
        }
    }

    public boolean isInitialized() {
        return initialized;
    }
}
