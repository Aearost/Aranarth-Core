package com.aearost.aranarthcore.event;

import org.bukkit.Bukkit;
import org.bukkit.entity.EntityType;
import org.bukkit.entity.Player;
import org.bukkit.entity.WanderingTrader;
import org.bukkit.event.EventHandler;
import org.bukkit.event.Listener;
import org.bukkit.event.entity.EntitySpawnEvent;

import com.aearost.aranarthcore.AranarthCore;
import com.aearost.aranarthcore.utils.ChatUtils;

public class WanderingTraderSpawnAnnounce implements Listener {

	public WanderingTraderSpawnAnnounce(AranarthCore plugin) {
		Bukkit.getPluginManager().registerEvents(this, plugin);
	}
	
	/**
	 * Deals with displaying a chat message when a wandering trader spawns nearby a player.
	 * @param e The event.
	 */
	@EventHandler
	public void onTraderSpawn(final EntitySpawnEvent e) {
		if (e.getEntityType() == EntityType.WANDERING_TRADER) {
			WanderingTrader wanderingTrader = (WanderingTrader) e.getEntity();
			for (Player player : Bukkit.getOnlinePlayers()) {
				if (player.getLocation().distance(wanderingTrader.getLocation()) <= 100) {
					Bukkit.broadcastMessage(ChatUtils.chatMessage("&7A wandering trader has spawned nearby &e" + player.getDisplayName()));
				}
			}
		}
	}

}
