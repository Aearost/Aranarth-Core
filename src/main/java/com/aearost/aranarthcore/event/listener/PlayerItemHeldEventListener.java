package com.aearost.aranarthcore.event.listener;

import com.aearost.aranarthcore.AranarthCore;
import org.bukkit.Bukkit;
import org.bukkit.entity.Player;
import org.bukkit.event.EventHandler;
import org.bukkit.event.Listener;
import org.bukkit.event.inventory.InventoryType;
import org.bukkit.event.player.PlayerItemHeldEvent;

/**
 * Centralizes all logic to be called when a player changes their held item slot.
 */
public class PlayerItemHeldEventListener implements Listener {

    public PlayerItemHeldEventListener(AranarthCore plugin) {
        Bukkit.getPluginManager().registerEvents(this, plugin);
    }

    @EventHandler
    public void onPlayerItemHeld(PlayerItemHeldEvent e) {
        Player player = e.getPlayer();
        // Prevents scrolling while in a held shulker box causing duplication bug
        if (player.getOpenInventory().getTitle().equals("Held Shulker")
                && player.getOpenInventory().getType() == InventoryType.CHEST) {
            e.setCancelled(true);
        }
    }
}
