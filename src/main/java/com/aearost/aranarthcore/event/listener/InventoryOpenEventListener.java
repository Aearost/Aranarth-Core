package com.aearost.aranarthcore.event.listener;

import com.aearost.aranarthcore.AranarthCore;
import com.aearost.aranarthcore.event.player.AfkCancelByInteract;
import com.aearost.aranarthcore.utils.AranarthUtils;
import org.bukkit.Bukkit;
import org.bukkit.entity.Player;
import org.bukkit.event.EventHandler;
import org.bukkit.event.Listener;
import org.bukkit.event.inventory.InventoryOpenEvent;
import org.bukkit.inventory.BlockInventoryHolder;

/**
 * Centralizes all logic to be called when a player opens an inventory.
 */
public class InventoryOpenEventListener implements Listener {

    public InventoryOpenEventListener(AranarthCore plugin) {
        Bukkit.getPluginManager().registerEvents(this, plugin);
    }

    @EventHandler
    public void onInventoryOpen(InventoryOpenEvent e) {
        if (e.getPlayer() instanceof Player player) {
            new AfkCancelByInteract().execute(player);
        }
        // Migrate any old Incantation items found in block containers
        if (e.getInventory().getHolder() instanceof BlockInventoryHolder) {
            AranarthUtils.migrateInventoryEssences(e.getInventory());
        }
    }
}
