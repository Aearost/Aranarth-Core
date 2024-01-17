package com.aearost.aranarthcore.event;

import org.bukkit.Bukkit;
import org.bukkit.entity.Player;
import org.bukkit.event.EventHandler;
import org.bukkit.event.Listener;
import org.bukkit.event.player.PlayerJoinEvent;

import com.aearost.aranarthcore.AranarthCore;
import com.aearost.aranarthcore.objects.AranarthPlayer;
import com.aearost.aranarthcore.utils.AranarthUtils;
import com.aearost.aranarthcore.utils.ChatUtils;

public class PlayerServerJoin implements Listener {

	public PlayerServerJoin(AranarthCore plugin) {
		Bukkit.getPluginManager().registerEvents(this, plugin);
	}

	/**
	 * Adds a new entry to the players HashMap if the player is not being tracked.
	 * Additionally customizes the join/leave server message format.
	 * 
	 * @param e
	 */
	@EventHandler
	public void onPlayerJoin(final PlayerJoinEvent e) {
		Player player = e.getPlayer();
		if (!AranarthUtils.hasPlayedBefore(player)) {
			AranarthUtils.addPlayer(player.getUniqueId(), new AranarthPlayer(player.getName()));
		}
		// If the player changed their username
		else if (!AranarthUtils.getUsername(player).equals(player.getName())) {
			AranarthUtils.setUsername(player);
		}
		
		if (!AranarthUtils.getNickname(player).equals("")) {
			e.setJoinMessage(ChatUtils.translateToColor("&8[&a+&8] " + AranarthUtils.getNickname(player)));
		} else {
			e.setJoinMessage(ChatUtils.translateToColor("&8[&a+&8] &7" + AranarthUtils.getUsername(player)));
		}
		
	}
	
}
