package com.aearost.aranarthcore.event.listener;

import com.aearost.aranarthcore.AranarthCore;
import com.aearost.aranarthcore.event.player.DominionChunkChange;
import com.aearost.aranarthcore.event.player.HomepadStep;
import com.aearost.aranarthcore.event.player.SpawnChangeLocation;
import com.aearost.aranarthcore.event.player.SpawnVoidFall;
import org.bukkit.Bukkit;
import org.bukkit.event.EventHandler;
import org.bukkit.event.Listener;
import org.bukkit.event.player.PlayerMoveEvent;

/**
 * Centralizes all logic to be called by a player moving.
 */
public class PlayerMoveEventListener implements Listener {

    public PlayerMoveEventListener(AranarthCore plugin) {
        Bukkit.getPluginManager().registerEvents(this, plugin);
    }

    @EventHandler
    public void onPlayerMove(PlayerMoveEvent e) {
        if (e.getPlayer().getLocation().getWorld().getName().startsWith("world")) {
            new DominionChunkChange().execute(e);
            new SpawnChangeLocation().execute(e);
        } else if (e.getPlayer().getLocation().getWorld().getName().startsWith("smp")) {
            new HomepadStep().execute(e);
        } else if (e.getPlayer().getLocation().getWorld().getName().startsWith("spawn")) {
            new SpawnVoidFall().execute(e);
        }
    }

}
