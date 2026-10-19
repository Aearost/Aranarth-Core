package com.aearost.aranarthcore.commands.general;

import com.aearost.aranarthcore.AranarthCore;
import com.aearost.aranarthcore.network.NetworkManager;
import com.aearost.aranarthcore.network.PendingTeleport;
import com.aearost.aranarthcore.objects.AranarthPlayer;
import com.aearost.aranarthcore.objects.CreativePlot;
import com.aearost.aranarthcore.utils.AranarthUtils;
import com.aearost.aranarthcore.utils.ChatUtils;
import com.aearost.aranarthcore.utils.Lang;
import com.aearost.aranarthcore.utils.PlotUtils;
import net.kyori.adventure.text.Component;
import net.kyori.adventure.text.event.ClickEvent;
import net.kyori.adventure.text.event.HoverEvent;
import net.kyori.adventure.text.serializer.legacy.LegacyComponentSerializer;
import org.bukkit.Bukkit;
import org.bukkit.Location;
import org.bukkit.World;
import org.bukkit.command.Command;
import org.bukkit.command.CommandExecutor;
import org.bukkit.command.CommandSender;
import org.bukkit.entity.Player;

import java.util.*;

/**
 * Handles claiming and managing plots in the Creative plots world.
 */
public class CommandPlot implements CommandExecutor {

    private static final long CONFIRM_MILLIS = 30_000;

    private record PendingConfirm(String action, String plotKey, long expiresAt) {
    }

    private static final Map<UUID, PendingConfirm> pendingConfirms = new HashMap<>();

    /**
     * @param sender The user that entered the command.
     * @param command The command itself.
     * @param alias The alias of the command.
     * @param args The arguments of the command.
     * @return Confirmation of whether the command was a success or not.
     */
    @Override
    public boolean onCommand(CommandSender sender, Command command, String alias, String[] args) {
        if (!(sender instanceof Player player)) {
            sender.sendMessage(ChatUtils.chatMessage(Lang.get("general.no_permission_console")));
            return true;
        }
        if (args.length == 0) {
            player.sendMessage(ChatUtils.chatMessage(Lang.get("general.invalid_syntax", "usage",
                    "plot <sub-command>")));
            return true;
        }

        switch (args[0].toLowerCase()) {
            case "claim" -> claim(player, args);
            case "unclaim" -> unclaim(player, args);
            case "reset" -> reset(player, args);
            case "merge" -> merge(player, args);
            case "unmerge" -> unmerge(player, args);
            case "rename" -> rename(player, args);
            case "add" -> add(player, args);
            case "remove" -> remove(player, args);
            case "home" -> home(player, args);
            case "tp" -> tp(player, args);
            case "list" -> list(player);
            case "info" -> info(player);
            case "spawn" -> spawn(player, args);
            default -> player.sendMessage(ChatUtils.chatMessage(Lang.get("general.invalid_syntax", "usage",
                    "plot <sub-command>")));
        }
        return true;
    }

    private void claim(Player player, String[] args) {
        if (args.length != 2) {
            player.sendMessage(ChatUtils.chatMessage(Lang.get("general.invalid_syntax", "usage", "plot claim <name>")));
            return;
        }
        int[] coordinates = getStandingPlotCoordinates(player);
        if (coordinates == null) {
            return;
        }
        if (PlotUtils.getPlot(coordinates[0], coordinates[1]) != null) {
            player.sendMessage(ChatUtils.chatMessage(Lang.get("plot.already_claimed")));
            return;
        }
        String name = args[1];
        if (!PlotUtils.isValidName(name)) {
            player.sendMessage(ChatUtils.chatMessage(Lang.get("plot.invalid_name")));
            return;
        }
        if (PlotUtils.getOwnedPlotByName(player.getUniqueId(), name) != null) {
            player.sendMessage(ChatUtils.chatMessage(Lang.get("plot.name_taken", "plot", name)));
            return;
        }
        AranarthPlayer aranarthPlayer = AranarthUtils.getPlayer(player.getUniqueId());
        int limit = PlotUtils.getPlotLimit(aranarthPlayer);
        if (PlotUtils.getOwnedPlots(player.getUniqueId()).size() >= limit) {
            player.sendMessage(ChatUtils.chatMessage(Lang.get("plot.limit_reached", "limit", limit)));
            return;
        }
        PlotUtils.claimPlot(coordinates[0], coordinates[1], player.getUniqueId(), name);
        player.sendMessage(ChatUtils.chatMessage(Lang.get("plot.claimed", "plot", name)));
    }

    private void unclaim(Player player, String[] args) {
        if (args.length == 2 && args[1].equalsIgnoreCase("confirm")) {
            PendingConfirm pending = takePendingConfirm(player, "unclaim");
            if (pending != null) {
                executeUnclaim(player, pending);
                return;
            }
        }
        // Running confirm without first requesting it acts on the target immediately
        boolean confirmed = isConfirmed(args);
        // Staff in admin mode can unclaim another player's plot by name
        if (args.length == 3 || (args.length == 4 && confirmed)) {
            CreativePlot plot = getOtherPlayersPlot(player, args);
            if (plot != null) {
                confirmOrRequest(player, "unclaim", plot.getKey(), plot, confirmed);
            }
            return;
        }
        CreativePlot plot = getManageablePlot(player);
        if (plot == null) {
            return;
        }
        confirmOrRequest(player, "unclaim", plot.getKey(), plot, confirmed);
    }

    private void reset(Player player, String[] args) {
        if (args.length == 2 && args[1].equalsIgnoreCase("confirm")) {
            PendingConfirm pending = takePendingConfirm(player, "reset");
            if (pending != null) {
                executeReset(player, pending);
                return;
            }
        }
        // Running confirm without first requesting it acts on the target immediately
        boolean confirmed = isConfirmed(args);

        // Staff in admin mode can reset another player's plot by name
        if (args.length == 3 || (args.length == 4 && confirmed)) {
            CreativePlot plot = getOtherPlayersPlot(player, args);
            if (plot != null) {
                confirmOrRequest(player, "reset", plot.getKey(), plot, confirmed);
            }
            return;
        }

        int[] coordinates = getStandingPlotCoordinates(player);
        if (coordinates == null) {
            return;
        }
        CreativePlot plot = PlotUtils.getPlot(coordinates[0], coordinates[1]);
        AranarthPlayer aranarthPlayer = AranarthUtils.getPlayer(player.getUniqueId());
        // Staff in admin mode can reset unclaimed plots too
        if (plot == null && aranarthPlayer.isInAdminMode()) {
            String key = PlotUtils.getKey(coordinates[0], coordinates[1]);
            confirmOrRequest(player, "reset", key, null, confirmed);
            return;
        }
        plot = getManageablePlot(player);
        if (plot == null) {
            return;
        }
        confirmOrRequest(player, "reset", plot.getKey(), plot, confirmed);
    }

    private void unmerge(Player player, String[] args) {
        if (args.length == 2 && args[1].equalsIgnoreCase("confirm")) {
            PendingConfirm pending = takePendingConfirm(player, "unmerge");
            if (pending != null) {
                executeUnmerge(player, pending);
                return;
            }
        }
        // Running confirm without first requesting it acts on the target immediately
        boolean confirmed = args.length >= 2 && args[args.length - 1].equalsIgnoreCase("confirm");
        int targetArgs = confirmed ? args.length - 1 : args.length;
        if (targetArgs > 2) {
            player.sendMessage(ChatUtils.chatMessage(Lang.get("general.invalid_syntax", "usage", "plot unmerge [name]")));
            return;
        }
        if (!isInPlotWorld(player)) {
            return;
        }
        CreativePlot plot;
        if (targetArgs == 2) {
            plot = PlotUtils.getOwnedPlotByName(player.getUniqueId(), args[1]);
            if (plot == null) {
                player.sendMessage(ChatUtils.chatMessage(Lang.get("plot.not_found", "plot", args[1])));
                return;
            }
        } else {
            plot = getManageablePlot(player);
            if (plot == null) {
                return;
            }
        }
        if (PlotUtils.getMergedGroup(plot).size() < 2) {
            player.sendMessage(ChatUtils.chatMessage(Lang.get("plot.not_merged", "plot", plot.getName())));
            return;
        }
        if (isGroupBusy(plot)) {
            player.sendMessage(ChatUtils.chatMessage(Lang.get("plot.busy")));
            return;
        }
        confirmOrRequest(player, "unmerge", plot.getKey(), plot, confirmed);
    }

    /**
     * Provides the plot of the confirmation if it is still claimed, manageable by the player, and not being regenerated.
     */
    private CreativePlot getConfirmedPlot(Player player, PendingConfirm pending) {
        CreativePlot plot = PlotUtils.getPlot(pending.plotKey());
        AranarthPlayer aranarthPlayer = AranarthUtils.getPlayer(player.getUniqueId());
        if (plot == null || (!player.getUniqueId().equals(plot.getOwner()) && !aranarthPlayer.isInAdminMode())) {
            player.sendMessage(ChatUtils.chatMessage(Lang.get("plot.nothing_to_confirm")));
            return null;
        }
        if (isGroupBusy(plot)) {
            player.sendMessage(ChatUtils.chatMessage(Lang.get("plot.busy")));
            return null;
        }
        return plot;
    }

    /**
     * Unclaims every plot of the confirmation's merged group.
     */
    private void executeUnclaim(Player player, PendingConfirm pending) {
        CreativePlot plot = getConfirmedPlot(player, pending);
        if (plot == null) {
            return;
        }
        String name = plot.getName();
        PlotUtils.unclaimGroup(plot, () -> {
            if (player.isOnline()) {
                player.sendMessage(ChatUtils.chatMessage(Lang.getFor(player, "plot.unclaimed", "plot", name)));
            }
        });
    }

    /**
     * Resets every plot of the confirmation's merged group, or the unclaimed plot area if nobody owns it.
     */
    private void executeReset(Player player, PendingConfirm pending) {
        // Staff in admin mode can reset unclaimed plots too
        if (PlotUtils.getPlot(pending.plotKey()) == null && AranarthUtils.getPlayer(player.getUniqueId()).isInAdminMode()) {
            String[] parts = pending.plotKey().split(";");
            PlotUtils.resetPlotArea(Integer.parseInt(parts[0]), Integer.parseInt(parts[1]), () -> {
                if (player.isOnline()) {
                    player.sendMessage(ChatUtils.chatMessage(Lang.getFor(player, "plot.reset", "plot", pending.plotKey())));
                }
            });
            return;
        }
        CreativePlot plot = getConfirmedPlot(player, pending);
        if (plot == null) {
            return;
        }
        String name = plot.getName();
        PlotUtils.resetGroup(plot, () -> {
            if (player.isOnline()) {
                player.sendMessage(ChatUtils.chatMessage(Lang.getFor(player, "plot.reset", "plot", name)));
            }
        });
    }

    /**
     * Unmerges the confirmation's merged group back into individual plots.
     */
    private void executeUnmerge(Player player, PendingConfirm pending) {
        CreativePlot plot = getConfirmedPlot(player, pending);
        if (plot == null) {
            return;
        }
        int count = PlotUtils.getMergedGroup(plot).size();
        if (count < 2) {
            player.sendMessage(ChatUtils.chatMessage(Lang.get("plot.not_merged", "plot", plot.getName())));
            return;
        }
        String name = plot.getName();
        PlotUtils.unmergeGroup(plot, () -> {
            if (player.isOnline()) {
                player.sendMessage(ChatUtils.chatMessage(Lang.getFor(player, "plot.unmerged", "plot", name, "count", count)));
            }
        });
    }

    private void merge(Player player, String[] args) {
        if (args.length != 3 && args.length != 4) {
            player.sendMessage(ChatUtils.chatMessage(Lang.get("general.invalid_syntax", "usage", "plot merge <plot1> <plot2> [name]")));
            return;
        }
        if (!isInPlotWorld(player)) {
            return;
        }
        CreativePlot first = PlotUtils.getOwnedPlotByName(player.getUniqueId(), args[1]);
        if (first == null) {
            player.sendMessage(ChatUtils.chatMessage(Lang.get("plot.not_found", "plot", args[1])));
            return;
        }
        CreativePlot second = PlotUtils.getOwnedPlotByName(player.getUniqueId(), args[2]);
        if (second == null) {
            player.sendMessage(ChatUtils.chatMessage(Lang.get("plot.not_found", "plot", args[2])));
            return;
        }
        if (first == second) {
            player.sendMessage(ChatUtils.chatMessage(Lang.get("plot.same_plot")));
            return;
        }
        List<CreativePlot> firstGroup = PlotUtils.getMergedGroup(first);
        List<CreativePlot> secondGroup = PlotUtils.getMergedGroup(second);
        if (firstGroup.contains(second)) {
            player.sendMessage(ChatUtils.chatMessage(Lang.get("plot.already_merged")));
            return;
        }

        String firstName = first.getName();
        String secondName = second.getName();
        String newName = args.length == 4 ? args[3] : firstName;
        if (!PlotUtils.isValidName(newName)) {
            player.sendMessage(ChatUtils.chatMessage(Lang.get("plot.invalid_name")));
            return;
        }
        // The new name may only already be used by one of the two plots being merged
        CreativePlot nameOwner = PlotUtils.getOwnedPlotByName(player.getUniqueId(), newName);
        if (nameOwner != null && !firstGroup.contains(nameOwner) && !secondGroup.contains(nameOwner)) {
            player.sendMessage(ChatUtils.chatMessage(Lang.get("plot.name_taken", "plot", newName)));
            return;
        }

        // Every pair of touching plots between the two groups is merged so the result has no roads left inside it
        List<CreativePlot[]> pairs = new ArrayList<>();
        for (CreativePlot a : firstGroup) {
            for (CreativePlot b : secondGroup) {
                if (Math.abs(a.getGridX() - b.getGridX()) + Math.abs(a.getGridZ() - b.getGridZ()) == 1) {
                    pairs.add(new CreativePlot[]{a, b});
                }
            }
        }
        if (pairs.isEmpty()) {
            player.sendMessage(ChatUtils.chatMessage(Lang.get("plot.not_adjacent")));
            return;
        }
        for (CreativePlot[] pair : pairs) {
            if (PlotUtils.isBusy(pair[0]) || PlotUtils.isBusy(pair[1])) {
                player.sendMessage(ChatUtils.chatMessage(Lang.get("plot.busy")));
                return;
            }
        }
        for (CreativePlot[] pair : pairs) {
            PlotUtils.mergePlots(pair[0], pair[1]);
        }
        PlotUtils.renameGroup(first, newName);
        player.sendMessage(ChatUtils.chatMessage(Lang.get("plot.merged", "plot", firstName, "other", secondName, "name", newName)));
    }

    private void rename(Player player, String[] args) {
        if (args.length != 3) {
            player.sendMessage(ChatUtils.chatMessage(Lang.get("general.invalid_syntax", "usage", "plot rename <current> <new>")));
            return;
        }
        CreativePlot plot = PlotUtils.getOwnedPlotByName(player.getUniqueId(), args[1]);
        if (plot == null) {
            player.sendMessage(ChatUtils.chatMessage(Lang.get("plot.not_found", "plot", args[1])));
            return;
        }
        String newName = args[2];
        if (!PlotUtils.isValidName(newName)) {
            player.sendMessage(ChatUtils.chatMessage(Lang.get("plot.invalid_name")));
            return;
        }
        // Allows changing only the capitalization of the plot's own name
        CreativePlot nameOwner = PlotUtils.getOwnedPlotByName(player.getUniqueId(), newName);
        if (nameOwner != null && !PlotUtils.getMergedGroup(plot).contains(nameOwner)) {
            player.sendMessage(ChatUtils.chatMessage(Lang.get("plot.name_taken", "plot", newName)));
            return;
        }
        String oldName = plot.getName();
        PlotUtils.renameGroup(plot, newName);
        player.sendMessage(ChatUtils.chatMessage(Lang.get("plot.renamed", "plot", oldName, "name", newName)));
    }

    private void add(Player player, String[] args) {
        if (args.length != 2) {
            player.sendMessage(ChatUtils.chatMessage(Lang.get("general.invalid_syntax", "usage", "plot add <player>")));
            return;
        }
        CreativePlot plot = getManageablePlot(player);
        if (plot == null) {
            return;
        }
        UUID targetUuid = AranarthUtils.getUUIDFromUsername(args[1]);
        if (targetUuid == null) {
            player.sendMessage(ChatUtils.chatMessage(Lang.get("player.not_found", "name", args[1])));
            return;
        }
        if (targetUuid.equals(plot.getOwner())) {
            player.sendMessage(ChatUtils.chatMessage(Lang.get("plot.cannot_add_owner")));
            return;
        }
        if (plot.getMembers().contains(targetUuid)) {
            player.sendMessage(ChatUtils.chatMessage(Lang.get("plot.member_already", "player", args[1], "plot", plot.getName())));
            return;
        }
        // Members apply to the whole merged group so they can build across the merged roads
        for (CreativePlot groupPlot : PlotUtils.getMergedGroup(plot)) {
            groupPlot.getMembers().add(targetUuid);
            PlotUtils.savePlot(groupPlot);
        }
        player.sendMessage(ChatUtils.chatMessage(Lang.get("plot.member_added", "player", args[1], "plot", plot.getName())));
    }

    private void remove(Player player, String[] args) {
        if (args.length != 2) {
            player.sendMessage(ChatUtils.chatMessage(Lang.get("general.invalid_syntax", "usage", "plot remove <player>")));
            return;
        }
        CreativePlot plot = getManageablePlot(player);
        if (plot == null) {
            return;
        }
        UUID targetUuid = AranarthUtils.getUUIDFromUsername(args[1]);
        if (targetUuid == null || !plot.getMembers().contains(targetUuid)) {
            player.sendMessage(ChatUtils.chatMessage(Lang.get("plot.member_not_added", "player", args[1], "plot", plot.getName())));
            return;
        }
        for (CreativePlot groupPlot : PlotUtils.getMergedGroup(plot)) {
            groupPlot.getMembers().remove(targetUuid);
            PlotUtils.savePlot(groupPlot);
        }
        player.sendMessage(ChatUtils.chatMessage(Lang.get("plot.member_removed", "player", args[1], "plot", plot.getName())));
    }

    private void home(Player player, String[] args) {
        if (args.length != 2) {
            player.sendMessage(ChatUtils.chatMessage(Lang.get("general.invalid_syntax", "usage", "plot home <name>")));
            return;
        }
        CreativePlot plot = PlotUtils.getOwnedPlotByName(player.getUniqueId(), args[1]);
        if (plot == null) {
            player.sendMessage(ChatUtils.chatMessage(Lang.get("plot.not_found", "plot", args[1])));
            return;
        }
        teleportToPlot(player, plot, "plot home " + PlotUtils.stripName(plot.getName()));
    }

    private void tp(Player player, String[] args) {
        if (args.length != 3) {
            player.sendMessage(ChatUtils.chatMessage(Lang.get("general.invalid_syntax", "usage", "plot tp <player> <plot>")));
            return;
        }
        UUID targetUuid = AranarthUtils.getUUIDFromUsername(args[1]);
        if (targetUuid == null) {
            player.sendMessage(ChatUtils.chatMessage(Lang.get("player.not_found", "name", args[1])));
            return;
        }
        CreativePlot plot = PlotUtils.getOwnedPlotByName(targetUuid, args[2]);
        if (plot == null) {
            player.sendMessage(ChatUtils.chatMessage(Lang.get("plot.not_found_player", "player", args[1], "plot", args[2])));
            return;
        }
        teleportToPlot(player, plot, "plot tp " + args[1] + " " + PlotUtils.stripName(plot.getName()));
    }

    /**
     * Teleports the player to the road along the south side of the plot, transferring to Survival first if needed.
     *
     * @param transferCommand The command to run on Survival when the player is transferred from the SMP.
     */
    private void teleportToPlot(Player player, CreativePlot plot, String transferCommand) {
        AranarthPlayer aranarthPlayer = AranarthUtils.getPlayer(player.getUniqueId());
        if (AranarthUtils.getTeleportTask(player.getUniqueId()) != null) {
            player.sendMessage(ChatUtils.chatMessage(Lang.get("general.already_teleporting")));
            return;
        }

        // The plots world lives on Survival - count down then transfer
        if (!PlotUtils.isPlotServer()) {
            transferToPlotServer(player, aranarthPlayer, transferCommand, "&e&l" + plot.getName());
            return;
        }

        World world = Bukkit.getWorld(PlotUtils.WORLD_NAME);
        if (world == null) {
            player.sendMessage(ChatUtils.chatMessage(Lang.get("teleport.could_not_creative")));
            return;
        }
        // The middle of the road along the south side of the plot, facing the plot
        int tileSize = PlotUtils.getPattern().getTileSize();
        int plotSize = PlotUtils.getPattern().getPlotSize();
        int x = plot.getGridX() * tileSize + plotSize / 2;
        int z = plot.getGridZ() * tileSize + plotSize + (tileSize - plotSize) / 2;
        int y = world.getHighestBlockYAt(x, z) + 1;
        Location destination = new Location(world, x + 0.5, y, z + 0.5, 180, 0);
        String name = plot.getName();
        AranarthUtils.teleportPlayer(player, player.getLocation(), destination, aranarthPlayer.isInAdminMode(),
                "&e&l" + name, Lang.get("plot.home", "plot", name), success -> {
                    if (success) {
                        player.sendMessage(ChatUtils.chatMessage(Lang.getFor(player, "plot.home", "plot", name)));
                    } else {
                        player.sendMessage(ChatUtils.chatMessage(Lang.getFor(player, "teleport.could_not_creative")));
                    }
                });
    }

    private void list(Player player) {
        AranarthPlayer aranarthPlayer = AranarthUtils.getPlayer(player.getUniqueId());
        List<CreativePlot> owned = PlotUtils.getOwnedPlots(player.getUniqueId());
        if (owned.isEmpty()) {
            player.sendMessage(ChatUtils.chatMessage(Lang.get("plot.list_empty")));
            return;
        }
        List<CreativePlot> groups = PlotUtils.getOwnedPlotGroups(player.getUniqueId());
        int limit = PlotUtils.getPlotLimit(aranarthPlayer);
        String limitText = limit == Integer.MAX_VALUE ? "∞" : String.valueOf(limit);
        player.sendMessage(ChatUtils.chatMessage(Lang.get("plot.list_header", "count", owned.size(), "limit", limitText)));
        int tileSize = PlotUtils.getPattern().getTileSize();
        for (CreativePlot plot : groups) {
            player.sendMessage(ChatUtils.translateToColor(Lang.get("plot.list_entry", "plot", plot.getName(),
                    "x", plot.getGridX() * tileSize, "z", plot.getGridZ() * tileSize)));
        }
    }

    private void info(Player player) {
        int[] coordinates = getStandingPlotCoordinates(player);
        if (coordinates == null) {
            return;
        }
        CreativePlot plot = PlotUtils.getPlot(coordinates[0], coordinates[1]);
        if (plot == null) {
            player.sendMessage(ChatUtils.chatMessage(Lang.get("plot.info_unclaimed")));
            return;
        }
        if (plot.isSpawn()) {
            player.sendMessage(ChatUtils.chatMessage(Lang.get("plot.info_spawn")));
            return;
        }
        String ownerName = AranarthUtils.getNickname(Bukkit.getOfflinePlayer(plot.getOwner()));
        player.sendMessage(ChatUtils.chatMessage(Lang.get("plot.info_owner", "plot", plot.getName(), "owner", ownerName)));
        if (!plot.getMembers().isEmpty()) {
            List<String> names = new ArrayList<>();
            for (UUID member : plot.getMembers()) {
                String memberName = Bukkit.getOfflinePlayer(member).getName();
                names.add(memberName != null ? memberName : member.toString());
            }
            player.sendMessage(ChatUtils.translateToColor(Lang.get("plot.info_members", "list", String.join(", ", names))));
        }
    }

    private void spawn(Player player, String[] args) {
        if (args.length == 1) {
            Bukkit.dispatchCommand(player, "creative");
            return;
        }
        AranarthPlayer aranarthPlayer = AranarthUtils.getPlayer(player.getUniqueId());
        if (aranarthPlayer.getCouncilRank() != 3) {
            player.sendMessage(ChatUtils.chatMessage(Lang.get("general.no_permission")));
            return;
        }
        if (args.length != 2 || (!args[1].equalsIgnoreCase("claim") && !args[1].equalsIgnoreCase("unclaim"))) {
            player.sendMessage(ChatUtils.chatMessage(Lang.get("general.invalid_syntax", "usage", "plot spawn [claim|unclaim]")));
            return;
        }
        int[] coordinates = getStandingPlotCoordinates(player);
        if (coordinates == null) {
            return;
        }
        CreativePlot plot = PlotUtils.getPlot(coordinates[0], coordinates[1]);
        if (args[1].equalsIgnoreCase("claim")) {
            if (plot != null) {
                player.sendMessage(ChatUtils.chatMessage(Lang.get("plot.already_claimed")));
                return;
            }
            PlotUtils.claimSpawnPlot(coordinates[0], coordinates[1]);
            player.sendMessage(ChatUtils.chatMessage(Lang.get("plot.spawn_claimed")));
        } else {
            if (plot == null || !plot.isSpawn()) {
                player.sendMessage(ChatUtils.chatMessage(Lang.get("plot.not_spawn")));
                return;
            }
            if (PlotUtils.isBusy(plot)) {
                player.sendMessage(ChatUtils.chatMessage(Lang.get("plot.busy")));
                return;
            }
            PlotUtils.unclaimSpawnPlot(plot, null);
            player.sendMessage(ChatUtils.chatMessage(Lang.get("plot.spawn_unclaimed")));
        }
    }

    /**
     * Provides whether the player is in the plots world, telling them how to get there otherwise.
     */
    private boolean isInPlotWorld(Player player) {
        if (!PlotUtils.isPlotWorld(player.getWorld())) {
            player.sendMessage(ChatUtils.chatMessage(Lang.get("plot.not_in_world")));
            return false;
        }
        return true;
    }

    /**
     * Provides the grid coordinates of the plot the player is standing in, or null with a message if there is none.
     */
    private int[] getStandingPlotCoordinates(Player player) {
        if (!isInPlotWorld(player)) {
            return null;
        }
        int[] coordinates = PlotUtils.getPlotCoordinates(player.getLocation());
        if (coordinates == null) {
            player.sendMessage(ChatUtils.chatMessage(Lang.get("plot.not_in_plot")));
        }
        return coordinates;
    }

    /**
     * Provides the plot the player is standing in if they own it, or if they are in admin mode.
     */
    private CreativePlot getManageablePlot(Player player) {
        int[] coordinates = getStandingPlotCoordinates(player);
        if (coordinates == null) {
            return null;
        }
        CreativePlot plot = PlotUtils.getPlot(coordinates[0], coordinates[1]);
        AranarthPlayer aranarthPlayer = AranarthUtils.getPlayer(player.getUniqueId());
        if (plot == null || plot.isSpawn()
                || (!player.getUniqueId().equals(plot.getOwner()) && !aranarthPlayer.isInAdminMode())) {
            player.sendMessage(ChatUtils.chatMessage(Lang.get("plot.not_owner")));
            return null;
        }
        if (PlotUtils.isBusy(plot)) {
            player.sendMessage(ChatUtils.chatMessage(Lang.get("plot.busy")));
            return null;
        }
        return plot;
    }

    /**
     * Provides another player's plot by name for staff in admin mode, or null with a message.
     *
     * @param args The command arguments, with the player at index 1 and the plot name at index 2.
     */
    private CreativePlot getOtherPlayersPlot(Player player, String[] args) {
        AranarthPlayer aranarthPlayer = AranarthUtils.getPlayer(player.getUniqueId());
        if (!aranarthPlayer.isInAdminMode()) {
            player.sendMessage(ChatUtils.chatMessage(Lang.get("general.no_permission")));
            return null;
        }
        if (!PlotUtils.isPlotServer()) {
            player.sendMessage(ChatUtils.chatMessage(Lang.get("plot.not_in_world")));
            return null;
        }
        UUID targetUuid = AranarthUtils.getUUIDFromUsername(args[1]);
        if (targetUuid == null) {
            player.sendMessage(ChatUtils.chatMessage(Lang.get("player.not_found", "name", args[1])));
            return null;
        }
        CreativePlot plot = PlotUtils.getOwnedPlotByName(targetUuid, args[2]);
        if (plot == null) {
            player.sendMessage(ChatUtils.chatMessage(Lang.get("plot.not_found_player", "player", args[1], "plot", args[2])));
            return null;
        }
        if (isGroupBusy(plot)) {
            player.sendMessage(ChatUtils.chatMessage(Lang.get("plot.busy")));
            return null;
        }
        return plot;
    }

    /**
     * Provides whether the plot or any plot merged with it is being regenerated.
     */
    private boolean isGroupBusy(CreativePlot plot) {
        for (CreativePlot groupPlot : PlotUtils.getMergedGroup(plot)) {
            if (PlotUtils.isBusy(groupPlot)) {
                return true;
            }
        }
        return false;
    }

    /**
     * Provides whether the command ends with confirm, either on its own or after a player and plot name.
     */
    private boolean isConfirmed(String[] args) {
        return (args.length == 2 || args.length == 4) && args[args.length - 1].equalsIgnoreCase("confirm");
    }

    /**
     * Runs the action immediately when already confirmed, otherwise asks the player to confirm it.
     */
    private void confirmOrRequest(Player player, String action, String plotKey, CreativePlot plot, boolean confirmed) {
        PendingConfirm pending = new PendingConfirm(action, plotKey, System.currentTimeMillis() + CONFIRM_MILLIS);
        if (confirmed) {
            pendingConfirms.remove(player.getUniqueId());
            switch (action) {
                case "reset" -> executeReset(player, pending);
                case "unclaim" -> executeUnclaim(player, pending);
                case "unmerge" -> executeUnmerge(player, pending);
            }
            return;
        }
        pendingConfirms.put(player.getUniqueId(), pending);
        String message;
        if (plot == null) {
            message = Lang.get("plot.confirm_" + action, "plot", plotKey);
        } else {
            // Warns that a reset or unclaim applies to every plot of a merged group
            int count = PlotUtils.getMergedGroup(plot).size();
            String messageKey = "plot.confirm_" + action + (count > 1 && !action.equals("unmerge") ? "_merged" : "");
            message = Lang.get(messageKey, "plot", plot.getName(), "count", count);
        }
        sendConfirmMessage(player, message, "/plot " + action + " confirm");
    }

    /**
     * Sends a confirmation prompt that runs the confirm command when clicked.
     */
    private void sendConfirmMessage(Player player, String prompt, String confirmCommand) {
        Component message = LegacyComponentSerializer.legacySection()
                .deserialize(ChatUtils.chatMessage(prompt))
                .clickEvent(ClickEvent.runCommand(confirmCommand))
                .hoverEvent(HoverEvent.showText(LegacyComponentSerializer.legacySection()
                        .deserialize(ChatUtils.translateToColor("&e" + confirmCommand))));
        player.sendMessage(message);
    }

    /**
     * Removes and provides the player's pending confirmation if it is for the action and has not expired.
     */
    private PendingConfirm takePendingConfirm(Player player, String action) {
        PendingConfirm pending = pendingConfirms.remove(player.getUniqueId());
        if (pending == null || !pending.action().equals(action) || System.currentTimeMillis() > pending.expiresAt()) {
            return null;
        }
        return pending;
    }

    /**
     * Counts down on the SMP, then transfers the player to Survival and runs the command there.
     */
    static void transferToPlotServer(Player player, AranarthPlayer aranarthPlayer, String commandToRun, String title) {
        if (!NetworkManager.isActive()) {
            player.sendMessage(ChatUtils.chatMessage(Lang.get("teleport.could_not_creative")));
            return;
        }
        AranarthUtils.teleportPlayer(player, player.getLocation(), player.getLocation(),
                aranarthPlayer.isInAdminMode(), title, "&7Transferring to Creative...", success -> {
                    if (success) {
                        PendingTeleport pt = PendingTeleport.forCommand(commandToRun, title, "&7You have teleported to Creative");
                        String survivalServerName = AranarthCore.getInstance().getConfig()
                                .getString("network.servers.survival", "survival");
                        NetworkManager.getInstance().saveInventoryAndTransfer(player, survivalServerName, pt);
                    }
                });
    }
}
