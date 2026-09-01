package com.aearost.aranarthcore.commands.general;

import com.aearost.aranarthcore.AranarthCore;
import com.aearost.aranarthcore.database.DatabaseManager;
import com.aearost.aranarthcore.gui.GuiReaper;
import com.aearost.aranarthcore.objects.AranarthPlayer;
import com.aearost.aranarthcore.utils.AranarthUtils;
import com.aearost.aranarthcore.utils.ChatUtils;
import com.aearost.aranarthcore.utils.ItemUtils;
import com.aearost.aranarthcore.utils.ReaperManager;
import org.bukkit.Bukkit;
import org.bukkit.Location;
import org.bukkit.World;
import org.bukkit.command.Command;
import org.bukkit.command.CommandExecutor;
import org.bukkit.command.CommandSender;
import org.bukkit.entity.Player;
import org.bukkit.inventory.ItemStack;

import java.io.IOException;
import java.util.List;

/**
 * Opens the Reaper Inventory GUI, allowing the player to purchase back items lost on their last death.
 */
public class CommandReaper implements CommandExecutor {

    // 24 hours in milliseconds
    private static final long EXPIRY_MS = 24L * 60 * 60 * 1000;

    @Override
    public boolean onCommand(CommandSender sender, Command command, String alias, String[] args) {
        if (!(sender instanceof Player player)) {
            sender.sendMessage(ChatUtils.chatMessage("&cYou must be a player to execute this command!"));
            return true;
        }

        Bukkit.getScheduler().runTaskAsynchronously(AranarthCore.getInstance(), () -> {
            String[] cached = ReaperManager.get(player.getUniqueId());
            String[] data = cached != null ? cached
                    : DatabaseManager.isActive() ? DatabaseManager.getInstance().loadReaperInventory(player.getUniqueId())
                    : null;

            Bukkit.getScheduler().runTask(AranarthCore.getInstance(), () -> {
                if (data == null) {
                    player.sendMessage(ChatUtils.chatMessage("&7You do not have a Reaper inventory"));
                    return;
                }

                long deathTime = Long.parseLong(data[1]);
                if (System.currentTimeMillis() - deathTime > EXPIRY_MS) {
                    ReaperManager.remove(player.getUniqueId());
                    Bukkit.getScheduler().runTaskAsynchronously(AranarthCore.getInstance(), () ->
                            DatabaseManager.getInstance().deleteReaperInventory(player.getUniqueId()));
                    player.sendMessage(ChatUtils.chatMessage("&7Your Reaper Inventory has expired"));
                    return;
                }

                ItemStack[] drops;
                try {
                    drops = ItemUtils.itemStackArrayFromBase64(data[0]);
                } catch (IOException e) {
                    player.sendMessage(ChatUtils.chatMessage("&cFailed to load your Reaper Inventory. Please contact the Council!"));
                    Bukkit.getLogger().warning(AranarthCore.LOG_PREFIX + "Failed to deserialize reaper drops for " + player.getName() + ": " + e.getMessage());
                    return;
                }

                // Parse death location - fall back to player's current location for old entries without location data
                Location deathLocation = player.getLocation();
                if (data.length >= 6 && data[2] != null && !data[2].isEmpty()) {
                    World deathWorld = Bukkit.getWorld(data[2]);
                    if (deathWorld != null) {
                        try {
                            double x = Double.parseDouble(data[3]);
                            double y = Double.parseDouble(data[4]);
                            double z = Double.parseDouble(data[5]);
                            deathLocation = new Location(deathWorld, x, y, z);
                        } catch (NumberFormatException ignored) {
                        }
                    }
                }

                AranarthPlayer aranarthPlayer = AranarthUtils.getPlayer(player.getUniqueId());
                double cost = getReaperCost(aranarthPlayer.getRank());

                GuiReaper gui = new GuiReaper(player, drops, cost, deathTime, deathLocation);
                gui.openGui(player);
            });
        });

        return true;
    }

    /**
     * Returns the flat reaper cost for the given rank (0-8), read from config under economy.reaper-costs.
     */
    public static double getReaperCost(int rank) {
        List<Double> costs = AranarthCore.getInstance().getConfig().getDoubleList("economy.reaper-costs");
        if (costs != null && rank < costs.size()) {
            return costs.get(rank);
        }
        // Fallback defaults if config entry is missing
        double[] defaults = {500, 2000, 5000, 15000, 30000, 100000, 400000, 700000, 1000000};
        return rank < defaults.length ? defaults[rank] : defaults[defaults.length - 1];
    }
}
