package com.aearost.aranarthcore.event.player;

import com.aearost.aranarthcore.objects.AranarthPlayer;
import com.aearost.aranarthcore.utils.AranarthUtils;
import com.aearost.aranarthcore.utils.ChatUtils;
import org.bukkit.Bukkit;
import org.bukkit.entity.Player;
import org.bukkit.event.player.PlayerMoveEvent;
import org.bukkit.scheduler.BukkitTask;

/**
 * Handles canceling a player's teleportation if they move during the teleportation attempt.
 */
public class PlayerTeleportCancelByMove {

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
			if (!AranarthUtils.locationsMatch(e.getFrom(), e.getTo())) {
				task.cancel();
				AranarthUtils.removeTeleportTask(player.getUniqueId());
				player.sendMessage(ChatUtils.chatMessage("&cYou cannot move when trying to teleport!"));

				// /ac tphere was sent
				if (aranarthPlayer.getTeleportToUuid() != null) {
					Bukkit.getLogger().info("A");
					Player target = Bukkit.getPlayer(aranarthPlayer.getTeleportToUuid());
					if (target != null) {
						target.sendMessage(ChatUtils.chatMessage("&e" + aranarthPlayer.getNickname() + " &cmoved and canceled the request"));
					}
				}
				// /ac tp was sent
				else {
					for (Player onlinePlayer : Bukkit.getOnlinePlayers()) {
						AranarthPlayer aranarthPlayerOnline = AranarthUtils.getPlayer(onlinePlayer.getUniqueId());
						if (aranarthPlayerOnline.getTeleportFromUuid() != null) {
							if (aranarthPlayerOnline.getTeleportFromUuid().equals(player.getUniqueId())) {
								onlinePlayer.sendMessage(ChatUtils.chatMessage("&e" + aranarthPlayer.getNickname() + " &cmoved and canceled the request"));
								return;
							}
						}
					}
				}
			}
		}
	}
}
