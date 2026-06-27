package com.aearost.aranarthcore.event.listener.misc;

import com.aearost.aranarthcore.AranarthCore;
import com.aearost.aranarthcore.utils.DefenderUtils;
import org.bukkit.Bukkit;
import org.bukkit.entity.Player;
import org.bukkit.event.EventHandler;
import org.bukkit.event.EventPriority;
import org.bukkit.event.Listener;
import org.bukkit.event.player.PlayerTeleportEvent;

/**
 * Handles the follow mode defender teleportation when a player changes worlds.
 */
public class DefenderFollowTeleportListener implements Listener {

    public DefenderFollowTeleportListener(AranarthCore plugin) {
        Bukkit.getPluginManager().registerEvents(this, plugin);
    }

    @EventHandler(priority = EventPriority.MONITOR, ignoreCancelled = true)
    public void onPlayerTeleport(PlayerTeleportEvent e) {
        Player player = e.getPlayer();

        if (e.getTo() == null) {
            return;
        }
        if (e.getFrom().getWorld() == null || e.getTo().getWorld() == null) {
            return;
        }
        if (e.getFrom().getWorld().equals(e.getTo().getWorld())) {
            return;
        }

        // Nothing to do if no followers
        if (DefenderUtils.getFollowDefenders(player.getUniqueId()).isEmpty()) {
            return;
        }

        String destWorld = e.getTo().getWorld().getName();
        if (!destWorld.startsWith("world")) {
            return;
        }

        // Delay by 1 tick so the player's world change is fully processed before teleporting entities
        Bukkit.getScheduler().runTaskLater(AranarthCore.getInstance(),
                () -> DefenderUtils.teleportFollowersToPlayer(player), 1L);
    }
}
