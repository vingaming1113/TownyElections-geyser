package com.townyelections.geyser;

import org.geysermc.api.GeyserAPI;
import org.geysermc.api.command.Command;
import org.geysermc.api.command.CommandExecutor;
import org.geysermc.api.command.CommandSource;
import org.geysermc.api.event.Subscribe;
import org.geysermc.api.event.command.GeyserDefineCommandsEvent;

import org.bukkit.Bukkit;
import org.bukkit.entity.Player;

import com.townyelections.TownyElections;
import com.townyelections.integration.TownyHook;
import com.townyelections.manager.ConfigManager;
import com.townyelections.manager.MessageManager;
import com.townyelections.model.Candidate;
import com.townyelections.model.Election;
import com.townyelections.model.ElectionPhase;

import java.util.ArrayList;
import java.util.List;
import java.util.UUID;

/**
 * Bedrock-specific election commands for GeyserMC.
 * This class registers and handles election commands specifically for Bedrock players.
 */
public class BedrockElectionCommands {

    private final TownyElectionsExtension extension;
    private final TownyElectionsBridge bridge;
    private final GeyserAPI geyserAPI;
    private TownyHook townyHook;
    private MessageManager messageManager;
    private ConfigManager configManager;

    public BedrockElectionCommands(TownyElectionsExtension extension, TownyElectionsBridge bridge) {
        this.extension = extension;
        this.bridge = bridge;
        this.geyserAPI = extension.getGeyserAPI();
    }

    public void initialize() {
        // Get TownyElections components
        Plugin plugin = extension.getTownyElectionsPlugin();
        if (plugin instanceof TownyElections) {
            TownyElections townyElections = (TownyElections) plugin;
            this.townyHook = townyElections.getTownyHook();
            this.messageManager = townyElections.getMessageManager();
            this.configManager = townyElections.getConfigManager();
        }
    }

    @Subscribe
    public void onDefineCommands(GeyserDefineCommandsEvent event) {
        // Register Bedrock-specific election commands
        registerElectionCommands(event);
    }

    private void registerElectionCommands(GeyserDefineCommandsEvent event) {
        // Command: /townyelections vote <candidate>
        Command voteCommand = Command.builder(extension)
                .name("vote")
                .bedrockOnly(true)
                .source(CommandSource.class)
                .aliases(List.of("v"))
                .description("Cast your vote in the town election")
                .executableOnConsole(false)
                .suggestedOpOnly(false)
                .permission("townyelections.vote")
                .executor(new VoteCommandExecutor())
                .build();

        // Command: /townyelections candidates
        Command candidatesCommand = Command.builder(extension)
                .name("candidates")
                .bedrockOnly(true)
                .source(CommandSource.class)
                .aliases(List.of("cand", "list"))
                .description("List all candidates in your town's election")
                .executableOnConsole(false)
                .suggestedOpOnly(false)
                .permission("townyelections.candidates")
                .executor(new CandidatesCommandExecutor())
                .build();

        // Command: /townyelections status
        Command statusCommand = Command.builder(extension)
                .name("status")
                .bedrockOnly(true)
                .source(CommandSource.class)
                .description("Check the status of your town's election")
                .executableOnConsole(false)
                .suggestedOpOnly(false)
                .permission("townyelections.status")
                .executor(new StatusCommandExecutor())
                .build();

        // Register the commands
        event.register(voteCommand);
        event.register(candidatesCommand);
        event.register(statusCommand);
        
        extension.logger().info("Registered Bedrock election commands");
    }

    private class VoteCommandExecutor implements CommandExecutor {
        @Override
        public void execute(CommandSource source, Command command, String[] args) {
            if (!bridge.isInitialized()) {
                source.sendMessage("§cTownyElections bridge is not initialized!");
                return;
            }

            if (args.length < 1) {
                source.sendMessage("§cUsage: /townyelections vote <candidate>");
                return;
            }

            String candidateName = String.join(" ", args);
            Player player = getPlayerFromSource(source);
            
            if (player == null) {
                source.sendMessage("§cOnly players can vote!");
                return;
            }

            UUID playerId = player.getUniqueId();
            
            // Get the player's town
            com.palmergames.bukkit.towny.object.Town town = townyHook.getPlayerTown(player);
            if (town == null) {
                source.sendMessage("§cYou are not part of any town!");
                return;
            }

            Election election = bridge.getElectionForTown(town.getUUID());
            if (election == null) {
                source.sendMessage("§cThere is no active election in your town!");
                return;
            }

            if (election.getPhase() != ElectionPhase.VOTING && 
                election.getPhase() != ElectionPhase.RUNOFF) {
                source.sendMessage("§cVoting is not currently open!");
                return;
            }

            // Find the candidate
            Candidate candidate = election.findCandidateByName(candidateName);
            if (candidate == null) {
                source.sendMessage("§cCandidate '" + candidateName + "' not found!");
                return;
            }

            // Cast the vote
            boolean success = bridge.castVote(playerId, town.getUUID(), candidate.getUuid());
            
            if (success) {
                source.sendMessage("§aVote cast successfully for " + candidate.getName() + "!");
            } else {
                source.sendMessage("§cFailed to cast vote. You may have already voted or are not eligible.");
            }
        }
    }

    private class CandidatesCommandExecutor implements CommandExecutor {
        @Override
        public void execute(CommandSource source, Command command, String[] args) {
            if (!bridge.isInitialized()) {
                source.sendMessage("§cTownyElections bridge is not initialized!");
                return;
            }

            Player player = getPlayerFromSource(source);
            
            if (player == null) {
                source.sendMessage("§cOnly players can use this command!");
                return;
            }

            // Get the player's town
            com.palmergames.bukkit.towny.object.Town town = townyHook.getPlayerTown(player);
            if (town == null) {
                source.sendMessage("§cYou are not part of any town!");
                return;
            }

            Election election = bridge.getElectionForTown(town.getUUID());
            if (election == null) {
                source.sendMessage("§cThere is no active election in your town!");
                return;
            }

            // List all candidates
            source.sendMessage("§6Candidates in " + election.getTownName() + " election:");
            
            List<Candidate> candidates = new ArrayList<>(election.getCandidateList());
            if (candidates.isEmpty()) {
                source.sendMessage("§7No candidates have registered yet.");
                return;
            }

            for (Candidate candidate : candidates) {
                String partyDisplay = election.getPartyColor(candidate.getPartyName());
                if (!partyDisplay.isEmpty()) {
                    source.sendMessage("§7- " + partyDisplay + candidate.getName());
                } else {
                    source.sendMessage("§7- " + candidate.getName());
                }
            }
            
            source.sendMessage("§7Total: " + candidates.size() + " candidate(s)");
        }
    }

    private class StatusCommandExecutor implements CommandExecutor {
        @Override
        public void execute(CommandSource source, Command command, String[] args) {
            if (!bridge.isInitialized()) {
                source.sendMessage("§cTownyElections bridge is not initialized!");
                return;
            }

            Player player = getPlayerFromSource(source);
            
            if (player == null) {
                source.sendMessage("§cOnly players can use this command!");
                return;
            }

            // Get the player's town
            com.palmergames.bukkit.towny.object.Town town = townyHook.getPlayerTown(player);
            if (town == null) {
                source.sendMessage("§cYou are not part of any town!");
                return;
            }

            Election election = bridge.getElectionForTown(town.getUUID());
            if (election == null) {
                source.sendMessage("§cThere is no active election in your town!");
                return;
            }

            // Show election status
            source.sendMessage("§6Election Status for " + election.getTownName());
            source.sendMessage("§7Phase: " + formatPhase(election.getPhase()));
            source.sendMessage("§7Voting System: " + election.getVotingSystem().name());
            source.sendMessage("§7Candidates: " + election.getCandidateCount());
            source.sendMessage("§7Votes Cast: " + election.getTotalVotes());
            
            long remaining = election.getMillisRemaining();
            if (remaining > 0) {
                source.sendMessage("§7Time Remaining: " + formatDuration(remaining));
            } else {
                source.sendMessage("§7Time Remaining: §cExpired");
            }
        }
    }

    /**
     * Get the Bukkit Player from a Geyser CommandSource
     */
    private Player getPlayerFromSource(CommandSource source) {
        if (source instanceof org.geysermc.api.command.BukkitCommandSource) {
            return ((org.geysermc.api.command.BukkitCommandSource) source).getBukkitSender();
        }
        return null;
    }

    /**
     * Format election phase for display
     */
    private String formatPhase(ElectionPhase phase) {
        return switch (phase) {
            case NOMINATION -> "§eNomination";
            case VOTING -> "§aVoting";
            case RUNOFF -> "§6Runoff";
            case CONCLUDED -> "§7Concluded";
            case CANCELLED -> "§cCancelled";
            default -> phase.name();
        };
    }

    /**
     * Format duration in milliseconds to a human-readable string
     */
    private String formatDuration(long millis) {
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
}
