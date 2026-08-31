package com.aearost.aranarthcore.event.player;

import com.aearost.aranarthcore.AranarthCore;
import com.aearost.aranarthcore.database.DatabaseManager;
import com.aearost.aranarthcore.network.NetworkManager;
import com.aearost.aranarthcore.objects.AranarthPlayer;
import com.aearost.aranarthcore.utils.*;
import org.bukkit.Bukkit;
import org.bukkit.Sound;
import org.bukkit.entity.Player;
import org.bukkit.event.inventory.InventoryClickEvent;
import org.bukkit.inventory.ItemStack;
import org.bukkit.inventory.meta.ItemMeta;

import java.text.NumberFormat;
import java.util.List;

/**
 * Handles clicks in the Reaper Inventory GUI.
 */
public class GuiReaperClick {

    public void execute(InventoryClickEvent e) {
        if (e.getClickedInventory() == null) {
            return;
        }

        e.setCancelled(true);

        int slot = e.getSlot();

        // Only act on the bottom bar buttons
        if (slot < 45) {
            return;
        }

        Player player = (Player) e.getWhoClicked();

        // Close button
        if (slot == 45) {
            player.playSound(player, Sound.ENTITY_ENDER_EYE_DEATH, 0.8F, 0.5F);
            player.closeInventory();
            return;
        }

        // Purchase button
        if (slot == 49) {
            ItemStack clicked = e.getClickedInventory().getItem(49);
            if (clicked == null || !clicked.hasItemMeta()) {
                return;
            }

            // Parse cost from the lore line
            ItemMeta meta = clicked.getItemMeta();
            List<String> lore = meta.getLore();
            if (lore == null || lore.isEmpty()) {
                return;
            }

            String costLine = ChatUtils.stripColorFormatting(lore.get(0));
            String priceStr = costLine.replace("Cost: $", "").replace(",", "").trim();
            double cost;
            try {
                cost = Double.parseDouble(priceStr);
            } catch (NumberFormatException ex) {
                return;
            }

            AranarthPlayer aranarthPlayer = AranarthUtils.getPlayer(player.getUniqueId());
            if (aranarthPlayer.getBalance() < cost) {
                NumberFormat nf = NumberFormat.getNumberInstance();
                player.sendMessage(ChatUtils.chatMessage("&cYou need &6$" + nf.format((long) cost) + " &cto recover your inventory!"));
                player.playSound(player, Sound.ENTITY_ENDER_EYE_DEATH, 0.8F, 0.5F);
                player.closeInventory();
                return;
            }

            // Collect items from display slots
            ItemStack[] drops = new ItemStack[45];
            for (int i = 0; i < 45; i++) {
                drops[i] = e.getView().getTopInventory().getItem(i);
            }

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
            player.sendMessage(ChatUtils.chatMessage("&aYour inventory has been recovered!"));
            player.closeInventory();
        }
    }
}
