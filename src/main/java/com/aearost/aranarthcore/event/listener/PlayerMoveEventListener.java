package com.aearost.aranarthcore.event.listener;

import com.aearost.aranarthcore.AranarthCore;
import com.aearost.aranarthcore.event.player.*;
import com.aearost.aranarthcore.utils.AranarthUtils;
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
        String worldName = e.getPlayer().getLocation().getWorld().getName();
        if (AranarthUtils.isSmpWorld(worldName)) {
            new HomepadStep().execute(e);
        } else if (worldName.startsWith("world")) {
            new DominionChunkChange().execute(e);
            new SpawnChangeLocation().execute(e);
        }

        new AfkCancelByMove().execute(e);
        new PlayerTeleportCancelByMove().execute(e);
    }

}
