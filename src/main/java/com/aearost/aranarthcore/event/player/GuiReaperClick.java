package com.aearost.aranarthcore.event.player;

import com.aearost.aranarthcore.AranarthCore;
import com.aearost.aranarthcore.database.DatabaseManager;
import com.aearost.aranarthcore.gui.GuiHolder;
import com.aearost.aranarthcore.gui.GuiReaper;
import com.aearost.aranarthcore.network.NetworkManager;
import com.aearost.aranarthcore.objects.AranarthPlayer;
import com.aearost.aranarthcore.utils.*;
import org.bukkit.Bukkit;
import org.bukkit.Location;
import org.bukkit.Sound;
import org.bukkit.entity.Player;
import org.bukkit.event.inventory.InventoryClickEvent;
import org.bukkit.inventory.ItemStack;

import java.text.NumberFormat;

/**
 * Handles clicks in the Reaper Inventory GUI.
 */
public class GuiReaperClick {

    public void execute(InventoryClickEvent e) {
        if (e.getClickedInventory() == null) {
            return;
        }

        e.setCancelled(true);

        // Only the buttons of the GUI itself count, never the player's own inventory
        if (e.getClickedInventory() != e.getView().getTopInventory()) {
            return;
        }
        GuiHolder holder = GuiHolder.of(e.getView().getTopInventory());
        if (holder == null || !(holder.getData() instanceof GuiReaper.ReaperState(
                ItemStack[] drops, double cost, Location deathLocation
        ))) {
            return;
        }

        int slot = e.getSlot();

        // Only act on the bottom bar buttons
        if (slot < 45) {
            return;
        }

        Player player = (Player) e.getWhoClicked();

        // Reaper inventory can only be claimed from a survival world
        if (slot != 45 && (player.getWorld() == null || !AranarthUtils.isSurvivalWorld(player.getWorld().getName()))) {
            player.sendMessage(ChatUtils.chatMessage(Lang.get("reaper.survival_only")));
            player.closeInventory();
            return;
        }

        // Close button
        if (slot == 45) {
            player.playSound(player, Sound.ENTITY_ENDER_EYE_DEATH, 0.8F, 0.5F);
            player.closeInventory();
            return;
        }

        // Drop to death location button
        if (slot == 47) {
            // A second click on the same GUI can never claim again
            holder.setData(null);

            // Load the chunk at the death location, then drop items there
            final Location finalDropLocation = deathLocation;
            Bukkit.getScheduler().runTask(AranarthCore.getInstance(), () -> {
                finalDropLocation.getWorld().getChunkAt(finalDropLocation).load(true);
                for (ItemStack item : drops) {
                    if (item != null) {
                        finalDropLocation.getWorld().dropItemNaturally(finalDropLocation, item.clone());
                    }
                }
            });

            // Delete from cache and DB
            ReaperManager.remove(player.getUniqueId());
            Bukkit.getScheduler().runTaskAsynchronously(AranarthCore.getInstance(), () ->
                    DatabaseManager.getInstance().deleteReaperInventory(player.getUniqueId()));

            player.playSound(player, Sound.ENTITY_ENDER_EYE_DEATH, 0.8F, 0.5F);
            player.sendMessage(ChatUtils.chatMessage(Lang.get("player.items_dropped")));
            player.closeInventory();
            return;
        }

        // Purchase button
        if (slot == 49) {
            if (!Double.isFinite(cost) || cost < 0) {
                return;
            }

            AranarthPlayer aranarthPlayer = AranarthUtils.getPlayer(player.getUniqueId());
            if (aranarthPlayer.getBalance() < cost) {
                NumberFormat nf = NumberFormat.getNumberInstance();
                player.sendMessage(ChatUtils.chatMessage(Lang.get("reaper.not_enough_money", "amount", nf.format((long) cost))));
                player.playSound(player, Sound.ENTITY_ENDER_EYE_DEATH, 0.8F, 0.5F);
                player.closeInventory();
                return;
            }

            holder.setData(null);

            // Deduct balance
            aranarthPlayer.setBalance(aranarthPlayer.getBalance() - cost);
            AranarthUtils.setPlayer(player.getUniqueId(), aranarthPlayer);
            PersistenceUtils.saveAranarthPlayerImmediately(player.getUniqueId());
            if (NetworkManager.isActive()) {
                NetworkManager.getInstance().publishBalanceAdjust(player.getUniqueId(), -cost);
            }

            // Give items back - drop at feet if no space
            for (ItemStack item : drops) {
                if (item != null) {
                    ItemStack copy = item.clone();
                    int remainder = ItemUtils.addToInventory(player, copy);
                    if (remainder == -1) {
                        // No space at all - drop full item
                        player.getWorld().dropItemNaturally(player.getLocation(), item.clone());
                    } else if (remainder > 0) {
                        // Partial - drop the leftover amount
                        ItemStack leftover = item.clone();
                        leftover.setAmount(remainder);
                        player.getWorld().dropItemNaturally(player.getLocation(), leftover);
                    }
                }
            }

            // Delete from cache and DB
            ReaperManager.remove(player.getUniqueId());
            Bukkit.getScheduler().runTaskAsynchronously(AranarthCore.getInstance(), () ->
                    DatabaseManager.getInstance().deleteReaperInventory(player.getUniqueId()));

            player.playSound(player, Sound.ENTITY_PLAYER_LEVELUP, 1.0F, 1.0F);
            player.sendMessage(ChatUtils.chatMessage(Lang.get("player.inventory_recovered")));
            player.closeInventory();
        }
    }
}
