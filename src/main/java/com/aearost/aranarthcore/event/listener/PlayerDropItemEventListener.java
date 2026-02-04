package com.aearost.aranarthcore.event.listener;

import com.aearost.aranarthcore.AranarthCore;
import com.aearost.aranarthcore.event.player.IncantationApply;
import org.bukkit.Bukkit;
import org.bukkit.event.EventHandler;
import org.bukkit.event.Listener;
import org.bukkit.event.player.PlayerDropItemEvent;

public class PlayerDropItemEventListener implements Listener {

    public PlayerDropItemEventListener(AranarthCore plugin) {
        Bukkit.getPluginManager().registerEvents(this, plugin);
    }

    /**
     * Centralizes all logic to be called when a player drops an item.
     */
    @EventHandler
    public void onItemDrop(PlayerDropItemEvent e) {
        new IncantationApply().execute(e);
    }
}
