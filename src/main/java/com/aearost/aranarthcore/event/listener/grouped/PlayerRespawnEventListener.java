package com.aearost.aranarthcore.event.listener.grouped;

import com.aearost.aranarthcore.AranarthCore;
import com.aearost.aranarthcore.event.player.RespawnNonSurvival;
import com.aearost.aranarthcore.event.player.RespawnSurvival;
import com.aearost.aranarthcore.objects.AranarthPlayer;
import com.aearost.aranarthcore.utils.AranarthUtils;
import org.bukkit.Bukkit;
import org.bukkit.entity.Player;
import org.bukkit.event.EventHandler;
import org.bukkit.event.Listener;
import org.bukkit.event.entity.PlayerDeathEvent;
import org.bukkit.event.player.PlayerRespawnEvent;

/**
 * Centralizes all logic to be called by clicking in an inventory.
 */
public class PlayerRespawnEventListener implements Listener {

    public PlayerRespawnEventListener(AranarthCore plugin) {
        Bukkit.getPluginManager().registerEvents(this, plugin);
    }

    /**
     * Saves the player's level and EXP before dying in the arena or creative world.
     * @param e The event.
     */
    @EventHandler
    public void onPlayerDeath(final PlayerDeathEvent e) {
        String world = e.getEntity().getWorld().getName();
        Player player = e.getEntity();
        if (world.equalsIgnoreCase("arena") || world.equalsIgnoreCase("creative")) {
            AranarthPlayer aranarthPlayer = AranarthUtils.getPlayer(player.getUniqueId());
            aranarthPlayer.setLevelBeforeDeath(player.getLevel());
            aranarthPlayer.setExpBeforeDeath(player.getExp());
            AranarthUtils.setPlayer(player.getUniqueId(), aranarthPlayer);
            e.getDrops().clear();
            e.setDroppedExp(0);
        } else {
            if (AranarthUtils.isArmorType(player, "soulbound")) {
                e.setKeepInventory(true);
                e.getDrops().clear();
                e.setDroppedExp(0);
                AranarthPlayer aranarthPlayer = AranarthUtils.getPlayer(player.getUniqueId());
                aranarthPlayer.setLevelBeforeDeath(player.getLevel());
                aranarthPlayer.setExpBeforeDeath(player.getExp());
                AranarthUtils.setPlayer(player.getUniqueId(), aranarthPlayer);
            }
        }
    }

    /**
     * Handles logic on the player's actual respawn.
     * @param e The event.
     */
    @EventHandler
    public void onPlayerRespawn(PlayerRespawnEvent e) {
        String world = e.getPlayer().getWorld().getName();
        if (world.equalsIgnoreCase("arena") || world.equalsIgnoreCase("creative")) {
            new RespawnNonSurvival().execute(e);
        } else {
            new RespawnSurvival().execute(e);
        }
    }
}
