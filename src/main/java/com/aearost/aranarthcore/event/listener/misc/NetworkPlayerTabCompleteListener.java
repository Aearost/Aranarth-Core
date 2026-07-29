package com.aearost.aranarthcore.event.listener.misc;

import com.aearost.aranarthcore.AranarthCore;
import com.aearost.aranarthcore.utils.AranarthUtils;
import com.destroystokyo.paper.event.server.AsyncTabCompleteEvent;
import org.bukkit.Bukkit;
import org.bukkit.event.EventHandler;
import org.bukkit.event.EventPriority;
import org.bukkit.event.Listener;

import java.util.List;
import java.util.Set;

/**
 * Provides network-aware tab completions (cross-server player names) for commands
 * that accept a player name. Runs at HIGHEST priority so it overrides any other
 * plugin or Paper-internal async completion handler that may have intercepted the
 * event before Bukkit's synchronous TabCompleter gets a chance to run.
 */
public class NetworkPlayerTabCompleteListener implements Listener {

    // Commands where argument 0 (the first argument after the command name) is always
    // a player name. Includes all known aliases so the lookup is O(1).
    private static final Set<String> PLAYER_ARG0_COMMANDS = Set.of(
        "balance", "bal",
        "countdown",
        "deaths",
        "info",
        "kills",
        "message", "msg", "tell",
        "pay",
        "pettransfer",
        "ping",
        "seen",
        "teleport", "tp",
        "trust",
        "untrust"
    );

    public NetworkPlayerTabCompleteListener(AranarthCore plugin) {
        plugin.getServer().getPluginManager().registerEvents(this, plugin);
    }

    /**
     * LOW priority — fires first, before any other plugin touches the event.
     * Pure diagnostic: logs the raw event state so we can see what Paper provides
     * before any handler modifies it.
     */
    @EventHandler(priority = EventPriority.LOW)
    public void onTabCompleteDiagnostic(AsyncTabCompleteEvent event) {
        if (!event.isCommand()) return;
        String buffer = event.getBuffer();
        String[] tokens = buffer.substring(1).split(" ", -1);
        if (tokens.length < 2) return;
        String command = tokens[0].toLowerCase();
        if (!PLAYER_ARG0_COMMANDS.contains(command) && !command.equals("mail")) return;

        Bukkit.getLogger().info(AranarthCore.LOG_PREFIX + "[TabDebug][AsyncTabComplete][LOW] buffer=\"" + buffer
                + "\" isHandled=" + event.isHandled()
                + " isCancelled=" + event.isCancelled()
                + " existingCompletions=" + event.getCompletions());
    }

    /**
     * HIGHEST priority — fires after all other plugins have had a chance to handle
     * the event. We overwrite completions with the full network player list so that
     * cross-server players always appear, regardless of what other plugins did.
     */
    @EventHandler(priority = EventPriority.HIGHEST)
    public void onTabComplete(AsyncTabCompleteEvent event) {
        if (!event.isCommand()) return;

        String buffer = event.getBuffer();
        // Split on spaces, keeping trailing empty strings so we know the current arg.
        String[] tokens = buffer.substring(1).split(" ", -1);
        if (tokens.length < 2) return;

        String command = tokens[0].toLowerCase();
        String partial = tokens[tokens.length - 1];

        // /command <player>  — player name is the only argument
        if (PLAYER_ARG0_COMMANDS.contains(command) && tokens.length == 2) {
            Bukkit.getLogger().info(AranarthCore.LOG_PREFIX + "[TabDebug][AsyncTabComplete][HIGHEST] command=\"" + command
                    + "\" partial=\"" + partial + "\" wasHandled=" + event.isHandled()
                    + " prevCompletions=" + event.getCompletions());
            applyPlayerCompletions(event, partial);
            return;
        }

        // /mail send <player>
        if (command.equals("mail") && tokens.length == 3
                && tokens[1].equalsIgnoreCase("send")) {
            Bukkit.getLogger().info(AranarthCore.LOG_PREFIX + "[TabDebug][AsyncTabComplete][HIGHEST] command=mail send"
                    + " partial=\"" + partial + "\" wasHandled=" + event.isHandled()
                    + " prevCompletions=" + event.getCompletions());
            applyPlayerCompletions(event, partial);
        }
    }

    private static void applyPlayerCompletions(AsyncTabCompleteEvent event, String partial) {
        List<String> names = AranarthUtils.getNetworkPlayerNames(partial);
        Bukkit.getLogger().info(AranarthCore.LOG_PREFIX + "[TabDebug][AsyncTabComplete] setting " + names.size()
                + " completion(s): " + names);
        event.setCompletions(names);
        event.setHandled(true);
    }
}
