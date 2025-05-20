package com.aearost.aranarthcore.event.mob;

import com.aearost.aranarthcore.utils.AranarthUtils;
import com.aearost.aranarthcore.utils.ChatUtils;
import org.bukkit.Bukkit;
import org.bukkit.entity.Player;
import org.bukkit.entity.WanderingTrader;
import org.bukkit.event.entity.EntitySpawnEvent;

/**
 * Deals with displaying a chat message when a wandering trader spawns nearby a player.
 */
public class WanderingTraderSpawnAnnounce {
	public void execute(EntitySpawnEvent e) {
		WanderingTrader wanderingTrader = (WanderingTrader) e.getEntity();
		for (Player player : Bukkit.getOnlinePlayers()) {
			if (player.getLocation().getWorld().getName().equalsIgnoreCase("world")) {
				if (player.getLocation().distance(wanderingTrader.getLocation()) <= 100) {
					Bukkit.broadcastMessage(ChatUtils.chatMessage("&7A wandering trader has spawned nearby &e" + AranarthUtils.getNickname(player)));
					return;
				}
			}
		}
	}
}
