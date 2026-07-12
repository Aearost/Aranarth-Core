package com.aearost.aranarthsmp.listeners;

import com.aearost.aranarthsmp.AranarthSMP;
import com.aearost.aranarthsmp.network.PendingTeleport;
import com.aearost.aranarthsmp.network.SMPNetworkManager;
import org.bukkit.Bukkit;
import org.bukkit.Location;
import org.bukkit.World;
import org.bukkit.entity.Player;
import org.bukkit.event.EventHandler;
import org.bukkit.event.EventPriority;
import org.bukkit.event.Listener;
import org.bukkit.event.player.PlayerJoinEvent;

import java.util.UUID;

public class PlayerJoinListener implements Listener {

    public PlayerJoinListener(AranarthSMP plugin) {
        Bukkit.getPluginManager().registerEvents(this, plugin);
    }

    @EventHandler(priority = EventPriority.LOWEST)
    public void onPlayerJoin(PlayerJoinEvent e) {
        Player player = e.getPlayer();
        SMPNetworkManager net = SMPNetworkManager.getInstance();
        if (net == null) return;

        // Announce to the network. On SMP we don't have AranarthPlayer data,
        // so we publish zeros for ranks — the survival server has the full data.
        // Survival's roster already contains the true rank from the player's last login there.
        net.publishPlayerJoin(player.getUniqueId(), player.getName(), 0, 0, 0, 0, false);

        // Update tab list with this player included
        net.updateTab();

        // Check for a pending teleport (e.g. arriving after /tp across servers)
        Bukkit.getScheduler().runTaskLater(AranarthSMP.getInstance(), () -> {
            PendingTeleport pending = net.getPendingTeleport(player.getUniqueId());
            if (pending == null) return;

            net.clearPendingTeleport(player.getUniqueId());

            if ("player".equals(pending.getType())) {
                Player target = Bukkit.getPlayer(UUID.fromString(pending.getTargetUuid()));
                if (target != null) {
                    player.teleport(target.getLocation());
                    if (pending.getTitleMain() != null && !pending.getTitleMain().isEmpty()) {
                        player.sendTitle(
                                pending.getTitleMain().replace("&", "§"),
                                pending.getTitleSub() != null ? pending.getTitleSub().replace("&", "§") : "",
                                10, 40, 10);
                    }
                }
            } else {
                World world = Bukkit.getWorld(pending.getWorld());
                if (world != null) {
                    Location dest = new Location(world,
                            pending.getX(), pending.getY(), pending.getZ(),
                            pending.getYaw(), pending.getPitch());
                    player.teleport(dest);
                    if (pending.getTitleMain() != null && !pending.getTitleMain().isEmpty()) {
                        player.sendTitle(
                                pending.getTitleMain().replace("&", "§"),
                                pending.getTitleSub() != null ? pending.getTitleSub().replace("&", "§") : "",
                                10, 40, 10);
                    }
                }
            }
        }, 2L);
    }
}
