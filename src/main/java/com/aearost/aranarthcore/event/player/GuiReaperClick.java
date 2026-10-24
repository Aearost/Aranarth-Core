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
                ItemStack[] drops, double cost, Location deathLocation, boolean deathLocationKnown, long deathTime,
                String[] record
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
            player.sendMessage(ChatUtils.chatMessage(Lang.getFor(player, "reaper.survival_only")));
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
            // The death world only exists on the server the player died on
            if (!deathLocationKnown) {
                player.sendMessage(ChatUtils.chatMessage(Lang.getFor(player, "reaper.wrong_server")));
                player.closeInventory();
                return;
            }

            // A second click on the same GUI can never claim again
            holder.setData(null);
            player.closeInventory();

            claim(player, deathTime, record, () -> {
                deathLocation.getWorld().getChunkAt(deathLocation).load(true);
                for (ItemStack item : drops) {
                    if (item != null) {
                        deathLocation.getWorld().dropItemNaturally(deathLocation, item.clone());
                    }
                }
                player.playSound(player, Sound.ENTITY_ENDER_EYE_DEATH, 0.8F, 0.5F);
                player.sendMessage(ChatUtils.chatMessage(Lang.getFor(player, "player.items_dropped")));
            }, () -> {
            });
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
                player.sendMessage(ChatUtils.chatMessage(Lang.getFor(player, "reaper.not_enough_money", "amount", nf.format((long) cost))));
                player.playSound(player, Sound.ENTITY_ENDER_EYE_DEATH, 0.8F, 0.5F);
                player.closeInventory();
                return;
            }

            holder.setData(null);
            player.closeInventory();

            // Charge first so the money cannot be spent while the claim is checked, and refund if the
            // Reaper Inventory was already claimed
            adjustBalance(player, -cost);
            claim(player, deathTime, record, () -> {
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
                player.playSound(player, Sound.ENTITY_PLAYER_LEVELUP, 1.0F, 1.0F);
                player.sendMessage(ChatUtils.chatMessage(Lang.getFor(player, "player.inventory_recovered")));
            }, () -> adjustBalance(player, cost));
        }
    }

    /**
     * Removes the Reaper Inventory from the database, and only runs onClaimed if this server is the one
     * that removed it.
     */
    private void claim(Player player, long deathTime, String[] record, Runnable onClaimed, Runnable onAlreadyClaimed) {
        ReaperManager.remove(player.getUniqueId());
        if (!DatabaseManager.isActive()) {
            onClaimed.run();
            return;
        }
        Bukkit.getScheduler().runTaskAsynchronously(AranarthCore.getInstance(), () -> {
            boolean claimed = DatabaseManager.getInstance().claimReaperInventory(player.getUniqueId(), deathTime);
            Bukkit.getScheduler().runTask(AranarthCore.getInstance(), () -> {
                if (claimed && player.isOnline()) {
                    onClaimed.run();
                } else {
                    onAlreadyClaimed.run();
                    if (claimed) {
                        // The player left before the items could be handed over, so put the Reaper Inventory back
                        restore(player, record);
                    } else if (player.isOnline()) {
                        player.sendMessage(ChatUtils.chatMessage(Lang.getFor(player, "reaper.no_inventory")));
                    }
                }
            });
        });
    }

    /**
     * Puts the Reaper Inventory back exactly as it was loaded, including where and on which server the
     * player died.
     */
    private void restore(Player player, String[] record) {
        try {
            String world = record.length >= 6 && record[2] != null ? record[2] : "world";
            double x = record.length >= 6 ? Double.parseDouble(record[3]) : 0;
            double y = record.length >= 6 ? Double.parseDouble(record[4]) : 64;
            double z = record.length >= 6 ? Double.parseDouble(record[5]) : 0;
            String server = record.length >= 7 ? record[6] : null;
            Bukkit.getScheduler().runTaskAsynchronously(AranarthCore.getInstance(), () ->
                    DatabaseManager.getInstance().upsertReaperInventory(player.getUniqueId(), record[0],
                            Long.parseLong(record[1]), world, x, y, z, server));
        } catch (Exception e) {
            Bukkit.getLogger().severe(AranarthCore.LOG_PREFIX + "Could not restore the Reaper Inventory of "
                    + player.getName() + ": " + e.getMessage());
        }
    }

    private void adjustBalance(Player player, double delta) {
        AranarthPlayer aranarthPlayer = AranarthUtils.getPlayer(player.getUniqueId());
        aranarthPlayer.setBalance(aranarthPlayer.getBalance() + delta);
        AranarthUtils.setPlayer(player.getUniqueId(), aranarthPlayer);
        PersistenceUtils.saveAranarthPlayerImmediately(player.getUniqueId());
        if (NetworkManager.isActive()) {
            NetworkManager.getInstance().publishBalanceAdjust(player.getUniqueId(), delta);
        }
    }
}
