package com.aearost.aranarthcore.event.listener.grouped;

import com.aearost.aranarthcore.AranarthCore;
import com.aearost.aranarthcore.utils.EnemyProximityUtils;
import org.bukkit.Bukkit;
import org.bukkit.Location;
import org.bukkit.entity.Player;
import org.bukkit.event.EventHandler;
import org.bukkit.event.EventPriority;
import org.bukkit.event.Listener;
import org.bukkit.event.player.PlayerJoinEvent;
import org.bukkit.event.player.PlayerMoveEvent;
import org.bukkit.event.player.PlayerQuitEvent;
import org.bukkit.event.player.PlayerRespawnEvent;
import org.bukkit.event.player.PlayerTeleportEvent;

/**
 * Keeps track of enemied dominion members near each dominion and outpost, so that an area's
 * bending flag is bypassed while an enemy is close by.
 */
public class EnemyProximityListener implements Listener {

    private static final long RESYNC_INTERVAL_TICKS = 100L;

    public EnemyProximityListener(AranarthCore plugin) {
        Bukkit.getPluginManager().registerEvents(this, plugin);
        Bukkit.getScheduler().runTaskTimer(plugin, () -> {
            for (Player player : Bukkit.getOnlinePlayers()) {
                EnemyProximityUtils.update(player);
            }
        }, RESYNC_INTERVAL_TICKS, RESYNC_INTERVAL_TICKS);
    }

    /**
     * Re-evaluates the player's proximity whenever they move into a new chunk.
     */
    @EventHandler(priority = EventPriority.MONITOR, ignoreCancelled = true)
    public void onMove(PlayerMoveEvent e) {
        Location from = e.getFrom();
        Location to = e.getTo();
        if ((from.getBlockX() >> 4) == (to.getBlockX() >> 4)
                && (from.getBlockZ() >> 4) == (to.getBlockZ() >> 4)
                && from.getWorld().equals(to.getWorld())) {
            return;
        }
        EnemyProximityUtils.update(e.getPlayer(), to);
    }

    /**
     * Re-evaluates the player's proximity at their teleport destination.
     */
    @EventHandler(priority = EventPriority.MONITOR, ignoreCancelled = true)
    public void onTeleport(PlayerTeleportEvent e) {
        EnemyProximityUtils.update(e.getPlayer(), e.getTo());
    }

    /**
     * Re-evaluates the player's proximity at their respawn location.
     */
    @EventHandler(priority = EventPriority.MONITOR)
    public void onRespawn(PlayerRespawnEvent e) {
        EnemyProximityUtils.update(e.getPlayer(), e.getRespawnLocation());
    }

    /**
     * Evaluates the player's proximity as soon as they join.
     */
    @EventHandler(priority = EventPriority.MONITOR)
    public void onJoin(PlayerJoinEvent e) {
        EnemyProximityUtils.update(e.getPlayer());
    }

    /**
     * Stops tracking the player once they leave.
     */
    @EventHandler(priority = EventPriority.MONITOR)
    public void onQuit(PlayerQuitEvent e) {
        EnemyProximityUtils.remove(e.getPlayer().getUniqueId());
    }
}
