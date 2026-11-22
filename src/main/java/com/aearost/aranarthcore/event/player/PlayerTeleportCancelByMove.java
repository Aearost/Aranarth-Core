package com.aearost.aranarthcore.event.player;

import com.aearost.aranarthcore.objects.AranarthPlayer;
import com.aearost.aranarthcore.utils.AranarthUtils;
import com.aearost.aranarthcore.utils.ChatUtils;
import com.aearost.aranarthcore.utils.Lang;
import org.bukkit.Bukkit;
import org.bukkit.Location;
import org.bukkit.entity.Player;
import org.bukkit.event.player.PlayerMoveEvent;
import org.bukkit.scheduler.BukkitTask;

/**
 * Handles canceling a player's teleportation if they move during the teleportation attempt.
 */
public class PlayerTeleportCancelByMove {

    // How many small position changes are tolerated during the countdown before it is cancelled
    private static final int MAX_MOVEMENTS = 1;

    public void execute(PlayerMoveEvent e) {
        // If they did not move to a different coordinate and only their mouse
        if (e.getTo() == null) {
            return;
        }

        Player player = e.getPlayer();
        AranarthPlayer aranarthPlayer = AranarthUtils.getPlayer(player.getUniqueId());
        BukkitTask task = AranarthUtils.getTeleportTask(player.getUniqueId());
        // If they're actively trying to teleport
        if (task != null) {
            Location anchor = AranarthUtils.getTeleportAnchor(player.getUniqueId());
            if (anchor == null) {
                anchor = e.getFrom();
            }
            Location to = e.getTo();
            // Measured from where the countdown began rather than per packet
            boolean positionChanged;
            if (anchor.getWorld() == null || !anchor.getWorld().equals(to.getWorld())) {
                positionChanged = true;
            } else {
                double dx = to.getX() - anchor.getX();
                double dz = to.getZ() - anchor.getZ();
                double horizontalThreshold = 0.5;
                double verticalThreshold = 1.5;
                positionChanged = (dx * dx + dz * dz) > horizontalThreshold * horizontalThreshold
                        || Math.abs(to.getY() - anchor.getY()) > verticalThreshold;
            }
            // Only one leftover movement is tolerated so players cannot shuffle around much
            if (!positionChanged) {
                Location from = e.getFrom();
                boolean bodyMoved = from.getX() != to.getX() || from.getY() != to.getY() || from.getZ() != to.getZ();
                if (bodyMoved && AranarthUtils.incrementTeleportMoveCount(player.getUniqueId()) > MAX_MOVEMENTS) {
                    positionChanged = true;
                }
            }
            if (positionChanged) {
                task.cancel();
                AranarthUtils.removeTeleportTask(player.getUniqueId());
                player.sendMessage(ChatUtils.chatMessage(Lang.getFor(player, "general.teleport_moved")));

                // /ac tphere was sent
                if (aranarthPlayer.getTeleportToUuid() != null) {
                    Player target = Bukkit.getPlayer(aranarthPlayer.getTeleportToUuid());
                    if (target != null) {
                        target.sendMessage(ChatUtils.chatMessage(Lang.getFor(target, "tp_request.moved_cancelled", "player", aranarthPlayer.getNickname())));
                    }
                }
                // /ac tp was sent
                else {
                    for (Player onlinePlayer : Bukkit.getOnlinePlayers()) {
                        AranarthPlayer aranarthPlayerOnline = AranarthUtils.getPlayer(onlinePlayer.getUniqueId());
                        if (aranarthPlayerOnline.getTeleportFromUuid() != null) {
                            if (aranarthPlayerOnline.getTeleportFromUuid().equals(player.getUniqueId())) {
                                onlinePlayer.sendMessage(ChatUtils.chatMessage(Lang.getFor(onlinePlayer, "tp_request.moved_cancelled", "player", aranarthPlayer.getNickname())));
                                return;
                            }
                        }
                    }
                }
            }
        }
    }
}
