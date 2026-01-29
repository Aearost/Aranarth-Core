package com.aearost.aranarthcore.event.mob;

import com.aearost.aranarthcore.utils.AranarthUtils;
import com.aearost.aranarthcore.utils.ChatUtils;
import org.bukkit.Bukkit;
import org.bukkit.entity.Player;
import org.bukkit.event.entity.EntityDeathEvent;

/**
 * Deals with displaying a chat message when a wandering trader is killed.
 */
public class WanderingTraderDeath {
	public void execute(final EntityDeathEvent e) {
		if (e.getDamageSource().getCausingEntity() instanceof Player player) {
			Bukkit.broadcastMessage(ChatUtils.chatMessage("&7A wandering trader was slain by " + AranarthUtils.getNickname(player)));
		} else {
			if (e.getDamageSource().getCausingEntity() != null) {
				Bukkit.broadcastMessage(ChatUtils.chatMessage("&7A wandering trader has been slain by &e" + e.getDamageSource().getCausingEntity().getName()));
			} else {
				Bukkit.broadcastMessage(ChatUtils.chatMessage("&7A wandering trader has died"));
			}
		}
	}
}
