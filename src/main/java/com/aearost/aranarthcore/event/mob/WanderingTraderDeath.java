package com.aearost.aranarthcore.event.mob;

import com.aearost.aranarthcore.AranarthCore;
import com.aearost.aranarthcore.utils.AranarthUtils;
import com.aearost.aranarthcore.utils.ChatUtils;
import org.bukkit.Bukkit;
import org.bukkit.entity.EntityType;
import org.bukkit.entity.Player;
import org.bukkit.event.EventHandler;
import org.bukkit.event.Listener;
import org.bukkit.event.entity.EntityDeathEvent;

public class WanderingTraderDeath implements Listener {

	public WanderingTraderDeath(AranarthCore plugin) {
		Bukkit.getPluginManager().registerEvents(this, plugin);
	}
	
	/**
	 * Deals with displaying a chat message when a wandering trader is killed.
	 * @param e The event.
	 */
	@EventHandler
	public void onTraderDeath(final EntityDeathEvent e) {
		if (e.getEntityType() == EntityType.WANDERING_TRADER) {
			if (e.getDamageSource().getCausingEntity() instanceof Player player) {
				Bukkit.broadcastMessage(ChatUtils.chatMessage("&7A wandering trader was slain by " + AranarthUtils.getNickname(player)));
			} else {
				Bukkit.broadcastMessage(ChatUtils.chatMessage("&7A wandering trader was slain by &e" + e.getDamageSource().getCausingEntity().getName()));
			}
		}
	}

}
