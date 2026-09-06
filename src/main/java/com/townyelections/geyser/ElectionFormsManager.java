package com.townyelections.geyser;

import org.geysermc.api.GeyserAPI;
import org.geysermc.api.connection.player.GeyserPlayer;
import org.geysermc.api.event.Subscribe;
import org.geysermc.api.event.bus.EventBus;
import org.geysermc.api.event.player.PlayerJoinEvent;
import org.geysermc.api.event.player.PlayerQuitEvent;
import org.geysermc.api.form.CustomForm;
import org.geysermc.api.form.SimpleForm;
import org.geysermc.api.form.FormResponse;

import org.bukkit.Bukkit;

import com.townyelections.model.Election;
import com.townyelections.model.ElectionPhase;
import com.townyelections.model.Candidate;

import java.util.UUID;
import java.util.List;
import java.util.HashMap;
import java.util.Map;

/**
 * Manages Bedrock Forms GUI for TownyElections.
 * Creates and handles native Bedrock forms for all election interactions.
 * 
 * This is the main feature of the extension - providing Bedrock players with
 * native Forms GUI instead of the Java inventory GUI.
 */
public class ElectionFormsManager {

    private final TownyElectionsExtension extension;
    private final GeyserAPI geyserAPI;
    private final TownyElectionsBridge bridge;
    private final Map<UUID, Long> lastFormSent = new HashMap<>();
    private boolean initialized = false;

    public ElectionFormsManager(TownyElectionsExtension extension, GeyserAPI geyserAPI, TownyElectionsBridge bridge) {
        this.extension = extension;
        this.geyserAPI = geyserAPI;
        this.bridge = bridge;
    }

    public void initialize() {
        if (initialized) {
            return;
        }

        EventBus eventBus = geyserAPI.eventBus();
        eventBus.subscribe(this);

        initialized = true;
        extension.logger().info("ElectionFormsManager initialized - Bedrock Forms GUI enabled");
    }

    public void shutdown() {
        if (!initialized) {
            return;
        }

        initialized = false;
        lastFormSent.clear();
        extension.logger().info("ElectionFormsManager shutdown");
    }

    @Subscribe
    public void onPlayerJoin(PlayerJoinEvent event) {
        UUID playerId = event.getPlayer().getUuid();
        
        // Only handle Bedrock players
        if (!bridge.isBedrockPlayer(playerId)) {
            return;
        }

        extension.logger().info("Bedrock player joined: " + event.getPlayer().getUsername() + 
                              " - Showing election form");
        
        // Send main election form after a short delay
        Bukkit.getScheduler().runTaskLater(extension.getTownyElectionsPlugin(), () -> {
            showMainElectionForm(playerId);
        }, 20L); // 1 second delay
    }

    @Subscribe
    public void onPlayerQuit(PlayerQuitEvent event) {
        UUID playerId = event.getPlayer().getUuid();
        lastFormSent.remove(playerId);
    }

    /**
     * Show the main election form to a Bedrock player
     * This automatically detects the election phase and shows the appropriate form
     */
    public void showMainElectionForm(UUID playerId) {
        if (!bridge.isInitialized()) {
            extension.logger().warning("Bridge not initialized, cannot show form");
            return;
        }

        Election election = bridge.getElectionForPlayer(playerId);
        
        if (election == null) {
            // No active election in player's town
            showNoElectionForm(playerId);
            return;
        }

        ElectionPhase phase = election.getPhase();
        
        switch (phase) {
            case NOMINATION:
                showNominationForm(playerId, election);
                break;
            case VOTING:
            case RUNOFF:
                showVotingForm(playerId, election);
                break;
            case CONCLUDED:
                showResultsForm(playerId, election);
                break;
            default:
                showNoElectionForm(playerId);
        }
    }

    /**
     * Show form when there's no active election in the player's town
     */
    private void showNoElectionForm(UUID playerId) {
        String townName = bridge.getTownName(playerId);
        
        GeyserPlayer geyserPlayer = bridge.getGeyserPlayer(playerId);
        if (geyserPlayer == null) {
            extension.logger().warning("Cannot get GeyserPlayer for " + playerId);
            return;
        }

        CustomForm.Builder formBuilder = CustomForm.builder();
        formBuilder.title("Town Elections");
        formBuilder.text("\u00a76No active election in " + townName);
        formBuilder.text("\u00a77Elections will appear here when they start.");
        formBuilder.button("\u00a7aOK", "close");
        
        sendForm(geyserPlayer, formBuilder.build());
    }

    /**
     * Show nomination phase form - displays current candidates
     */
    private void showNominationForm(UUID playerId, Election election) {
        String townName = bridge.getTownName(playerId);
        long timeRemaining = bridge.getTimeRemaining(playerId);
        String timeStr = formatTime(timeRemaining);

        GeyserPlayer geyserPlayer = bridge.getGeyserPlayer(playerId);
        if (geyserPlayer == null) {
            return;
        }

        CustomForm.Builder formBuilder = CustomForm.builder();
        formBuilder.title("\u00a76Town Election - " + townName);
        formBuilder.text("\u00a7eNomination Phase");
        formBuilder.text("\u00a77Time remaining: " + timeStr);
        
        List<Candidate> candidates = bridge.getCandidates(playerId);
        if (candidates.isEmpty()) {
            formBuilder.text("\u00a7cNo candidates have registered yet.");
        } else {
            formBuilder.text("\u00a7aCurrent Candidates:");
            for (Candidate candidate : candidates) {
                formBuilder.text("\u00a77- " + candidate.getName());
            }
        }
        
        formBuilder.button("\u00a7aRefresh", "refresh");
        formBuilder.button("\u00a7cClose", "close");
        
        sendForm(geyserPlayer, formBuilder.build());
    }

    /**
     * Show voting form with candidate selection buttons
     * This is the main voting interface for Bedrock players
     */
    private void showVotingForm(UUID playerId, Election election) {
        String townName = bridge.getTownName(playerId);
        long timeRemaining = bridge.getTimeRemaining(playerId);
        String timeStr = formatTime(timeRemaining);
        boolean hasVoted = bridge.hasVoted(playerId);

        GeyserPlayer geyserPlayer = bridge.getGeyserPlayer(playerId);
        if (geyserPlayer == null) {
            return;
        }

        if (hasVoted) {
            showAlreadyVotedForm(playerId, election);
            return;
        }

        List<Candidate> candidates = bridge.getCandidates(playerId);
        
        if (candidates.isEmpty()) {
            showNoCandidatesForm(playerId);
            return;
        }

        // Use SimpleForm for candidate selection (one button per candidate)
        SimpleForm.Builder formBuilder = SimpleForm.builder();
        formBuilder.title("\u00a76Vote in " + townName + " Election");
        formBuilder.text("\u00a77Time remaining: " + timeStr);
        formBuilder.text("\u00a7aSelect a candidate to vote for:");
        
        // Add a button for each candidate
        for (Candidate candidate : candidates) {
            String party = candidate.getPartyName();
            String displayName = candidate.getName();
            if (party != null && !party.equals("Independent") && !party.isEmpty()) {
                displayName = "\u00a7e[" + party + "] \u00a7f" + candidate.getName();
            }
            formBuilder.button(displayName, "vote_" + candidate.getUuid().toString());
        }
        
        // Add cancel button
        formBuilder.button("\u00a7cCancel", "close");
        
        // Set form handler for button clicks
        formBuilder.respondWith((response, form) -> {
            handleVotingFormResponse(playerId, response);
        });
        
        sendForm(geyserPlayer, formBuilder.build());
    }

    /**
     * Handle response from voting form
     */
    private void handleVotingFormResponse(UUID playerId, FormResponse response) {
        String clickedButton = response.getClickedButton().getValue();
        
        if (clickedButton.startsWith("vote_")) {
            String candidateIdStr = clickedButton.substring(5);
            try {
                UUID candidateId = UUID.fromString(candidateIdStr);
                boolean success = bridge.castVote(playerId, candidateId);
                if (success) {
                    showVoteConfirmationForm(playerId, candidateId);
                } else {
                    showVoteFailedForm(playerId);
                }
            } catch (Exception e) {
                showVoteFailedForm(playerId);
            }
        }
    }

    /**
     * Show form when player has already voted
     */
    private void showAlreadyVotedForm(UUID playerId, Election election) {
        Candidate votedCandidate = null;
        List<UUID> ballot = election.getBallot(playerId);
        if (!ballot.isEmpty()) {
            votedCandidate = election.getCandidate(ballot.get(0));
        }

        GeyserPlayer geyserPlayer = bridge.getGeyserPlayer(playerId);
        if (geyserPlayer == null) {
            return;
        }

        CustomForm.Builder formBuilder = CustomForm.builder();
        formBuilder.title("\u00a7aVote Cast!");
        
        if (votedCandidate != null) {
            formBuilder.text("\u00a7aYou have already voted for:");
            formBuilder.text("\u00a77\u00a7l" + votedCandidate.getName() + "\u00a7r");
        } else {
            formBuilder.text("\u00a7aYou have already voted.");
        }
        
        formBuilder.button("\u00a7aOK", "close");
        
        sendForm(geyserPlayer, formBuilder.build());
    }

    /**
     * Show vote confirmation form after successful vote
     */
    private void showVoteConfirmationForm(UUID playerId, UUID candidateId) {
        Candidate candidate = bridge.getCandidate(candidateId);
        String candidateName = candidate != null ? candidate.getName() : "Unknown";

        GeyserPlayer geyserPlayer = bridge.getGeyserPlayer(playerId);
        if (geyserPlayer == null) {
            return;
        }

        CustomForm.Builder formBuilder = CustomForm.builder();
        formBuilder.title("\u00a7aVote Cast Successfully!");
        formBuilder.text("\u00a77You voted for: \u00a7e" + candidateName);
        formBuilder.text("\u00a7aThank you for participating!");
        formBuilder.button("\u00a7aOK", "close");
        
        sendForm(geyserPlayer, formBuilder.build());
    }

    /**
     * Show vote failed form
     */
    private void showVoteFailedForm(UUID playerId) {
        GeyserPlayer geyserPlayer = bridge.getGeyserPlayer(playerId);
        if (geyserPlayer == null) {
            return;
        }

        CustomForm.Builder formBuilder = CustomForm.builder();
        formBuilder.title("\u00a7cVote Failed");
        formBuilder.text("\u00a7cCould not cast your vote.");
        formBuilder.text("\u00a77You may have already voted or are not eligible.");
        formBuilder.button("\u00a7aOK", "close");
        
        sendForm(geyserPlayer, formBuilder.build());
    }

    /**
     * Show form when there are no candidates to vote for
     */
    private void showNoCandidatesForm(UUID playerId) {
        GeyserPlayer geyserPlayer = bridge.getGeyserPlayer(playerId);
        if (geyserPlayer == null) {
            return;
        }

        CustomForm.Builder formBuilder = CustomForm.builder();
        formBuilder.title("\u00a7cNo Candidates");
        formBuilder.text("\u00a7cThere are no candidates in this election.");
        formBuilder.text("\u00a77Check back later when candidates register.");
        formBuilder.button("\u00a7aOK", "close");
        
        sendForm(geyserPlayer, formBuilder.build());
    }

    /**
     * Show election results form when election has concluded
     */
    private void showResultsForm(UUID playerId, Election election) {
        String townName = bridge.getTownName(playerId);
        int totalVotes = election.getTotalVotes();

        GeyserPlayer geyserPlayer = bridge.getGeyserPlayer(playerId);
        if (geyserPlayer == null) {
            return;
        }

        CustomForm.Builder formBuilder = CustomForm.builder();
        formBuilder.title("\u00a76Election Results - " + townName);
        formBuilder.text("\u00a7aElection has concluded!");
        formBuilder.text("\u00a77Total votes cast: " + totalVotes);
        
        // Show candidates with vote counts
        List<Candidate> candidates = bridge.getCandidates(playerId);
        if (!candidates.isEmpty()) {
            formBuilder.text("\u00a76Results:");
            Map<UUID, Integer> tally = election.tally();
            for (Candidate candidate : candidates) {
                int votes = tally.getOrDefault(candidate.getUuid(), 0);
                formBuilder.text("\u00a77" + candidate.getName() + ": " + votes + " votes");
            }
        }
        
        formBuilder.button("\u00a7aOK", "close");
        
        sendForm(geyserPlayer, formBuilder.build());
    }

    /**
     * Send a form to a GeyserPlayer
     */
    private void sendForm(GeyserPlayer geyserPlayer, Object form) {
        try {
            if (form instanceof CustomForm) {
                geyserPlayer.sendForm((CustomForm) form);
            } else if (form instanceof SimpleForm) {
                geyserPlayer.sendForm((SimpleForm) form);
            }
        } catch (Exception e) {
            extension.logger().warning("Error sending form to " + geyserPlayer.getUsername() + ": " + e.getMessage());
        }
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
