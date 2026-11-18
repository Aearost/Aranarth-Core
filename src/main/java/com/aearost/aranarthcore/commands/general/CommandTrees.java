package com.aearost.aranarthcore.commands.general;

import com.aearost.aranarthcore.gui.GuiTrees;
import com.aearost.aranarthcore.trees.WildGrowth;
import com.aearost.aranarthcore.utils.AranarthUtils;
import com.aearost.aranarthcore.utils.ChatUtils;
import com.aearost.aranarthcore.utils.Lang;
import org.bukkit.Bukkit;
import org.bukkit.World;
import org.bukkit.command.Command;
import org.bukkit.command.CommandExecutor;
import org.bukkit.command.CommandSender;
import org.bukkit.entity.Player;

import java.util.List;

/**
 * Opens the GUI where a player chooses which custom trees their saplings may grow into, or to generate plants around the world.
 */
public class CommandTrees implements CommandExecutor {

    @Override
    public boolean onCommand(CommandSender sender, Command command, String alias, String[] args) {
        if (args.length > 0 && args[0].equalsIgnoreCase("regen")) {
            regen(sender, args);
            return true;
        }
        if (!(sender instanceof Player player)) {
            sender.sendMessage(ChatUtils.chatMessage(Lang.getDefault("general.no_permission_console")));
            return true;
        }
        new GuiTrees(player, 0).openGui();
        return true;
    }

    /**
     * Handles the /trees regen command.
     */
    private void regen(CommandSender sender, String[] args) {
        if (!sender.hasPermission("aranarth.trees.regen")) {
            sender.sendMessage(ChatUtils.chatMessage(Lang.getFor(sender, "general.no_permission")));
            return;
        }
        if (args.length < 2) {
            sender.sendMessage(ChatUtils.chatMessage(Lang.getFor(sender, "trees.regen_usage")));
            return;
        }
        String name = args[1];
        String action = args.length > 2 ? args[2].toLowerCase() : "start";
        switch (action) {
            case "stop" -> {
                String key = WildGrowth.stop(name) ? "trees.regen_stopped" : "trees.regen_not_running";
                sender.sendMessage(ChatUtils.chatMessage(Lang.getFor(sender, key, "world", name)));
            }
            case "status" -> {
                int[] progress = WildGrowth.getProgress(name);
                if (progress == null) {
                    sender.sendMessage(ChatUtils.chatMessage(Lang.getFor(sender, "trees.regen_not_running", "world", name)));
                    return;
                }
                sender.sendMessage(ChatUtils.chatMessage(Lang.getFor(sender, "trees.regen_status", "world", name,
                        "region", String.valueOf(progress[0]), "regions", String.valueOf(progress[1]),
                        "groups", String.valueOf(progress[2]))));
            }
            case "near" -> {
                if (!(sender instanceof Player player)) {
                    sender.sendMessage(ChatUtils.chatMessage(Lang.getDefault("general.no_permission_console")));
                    return;
                }
                List<WildGrowth.Grown> nearest = WildGrowth.findNearest(name, player.getLocation().getBlockX(),
                        player.getLocation().getBlockZ(), 8);
                if (nearest.isEmpty()) {
                    player.sendMessage(ChatUtils.chatMessage(Lang.getFor(player, "trees.regen_near_none", "world", name)));
                    return;
                }
                player.sendMessage(ChatUtils.chatMessage(Lang.getFor(player, "trees.regen_near", "world", name)));
                boolean sameWorld = player.getWorld().getName().equals(name);
                for (WildGrowth.Grown grown : nearest) {
                    String distance = sameWorld ? String.valueOf((int) Math.hypot(grown.x() - player.getLocation().getX(),
                            grown.z() - player.getLocation().getZ())) : "?";
                    player.sendMessage(ChatUtils.translateToColor(Lang.getFor(player, "trees.regen_near_entry",
                            "plant", Lang.getFor(player, "produce." + grown.plant()), "count", String.valueOf(grown.count()),
                            "x", String.valueOf(grown.x()), "y", String.valueOf(grown.y()), "z", String.valueOf(grown.z()),
                            "distance", distance)));
                }
            }
            case "start" -> {
                World world = Bukkit.getWorld(name);
                if (world == null) {
                    sender.sendMessage(ChatUtils.chatMessage(Lang.getFor(sender, "admin.world_not_found", "name", name)));
                    return;
                }
                // Only the worlds players survive in, leaving out spawn and the shop islands
                if (!AranarthUtils.isSurvivalWorld(name) || name.equals("spawn") || name.equals("shops")
                        || world.getEnvironment() != World.Environment.NORMAL) {
                    sender.sendMessage(ChatUtils.chatMessage(Lang.getFor(sender, "trees.regen_world_invalid")));
                    return;
                }
                if (!WildGrowth.start(world, sender instanceof Player player ? player.getUniqueId() : null)) {
                    sender.sendMessage(ChatUtils.chatMessage(Lang.getFor(sender, "trees.regen_already", "world", name)));
                    return;
                }
                sender.sendMessage(ChatUtils.chatMessage(Lang.getFor(sender, "trees.regen_started", "world", name)));
            }
            default -> sender.sendMessage(ChatUtils.chatMessage(Lang.getFor(sender, "trees.regen_usage")));
        }
    }
}
