package com.aearost.aranarthcore.event.listener;

import com.aearost.aranarthcore.AranarthCore;
import com.aearost.aranarthcore.event.player.HoneyGlazedHamEat;
import org.bukkit.Bukkit;
import org.bukkit.Material;
import org.bukkit.event.EventHandler;
import org.bukkit.event.Listener;
import org.bukkit.event.player.PlayerItemConsumeEvent;

/**
 * Centralizes all logic to be called by a player consuming an item.
 */
public class PlayerItemConsumeEventListener implements Listener {

    public PlayerItemConsumeEventListener(AranarthCore plugin) {
        Bukkit.getPluginManager().registerEvents(this, plugin);
    }

    @EventHandler
    public void onItemConsume(PlayerItemConsumeEvent e) {
        if (e.getItem().getType() == Material.COOKED_PORKCHOP) {
            new HoneyGlazedHamEat().execute(e);
        }
    }
}
