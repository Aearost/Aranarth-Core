package com.aearost.aranarthcore.event.listener.grouped;

import com.aearost.aranarthcore.AranarthCore;
import org.bukkit.Bukkit;
import org.bukkit.GameMode;
import org.bukkit.entity.Player;
import org.bukkit.event.EventHandler;
import org.bukkit.event.Listener;
import org.bukkit.event.player.PlayerGameModeChangeEvent;

/**
 * Resets walk and fly speed to default when a player enters Survival mode.
 */
public class PlayerGameModeChangeEventListener implements Listener {

    public PlayerGameModeChangeEventListener(AranarthCore plugin) {
        Bukkit.getPluginManager().registerEvents(this, plugin);
    }

    @EventHandler
    public void onGameModeChange(PlayerGameModeChangeEvent e) {
        if (e.getNewGameMode() == GameMode.SURVIVAL) {
            Player player = e.getPlayer();
            player.setWalkSpeed(0.2f);
            player.setFlySpeed(0.1f);
        }
    }
}
