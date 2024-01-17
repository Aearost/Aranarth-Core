package com.aearost.aranarthcore.event;

import org.bukkit.Bukkit;
import org.bukkit.entity.Player;
import org.bukkit.event.EventHandler;
import org.bukkit.event.Listener;
import org.bukkit.event.player.PlayerQuitEvent;

import com.aearost.aranarthcore.AranarthCore;
import com.aearost.aranarthcore.utils.AranarthUtils;
import com.aearost.aranarthcore.utils.ChatUtils;

public class PlayerServerLeave implements Listener {

	public PlayerServerLeave(AranarthCore plugin) {
		Bukkit.getPluginManager().registerEvents(this, plugin);
	}

	/**
	 * Adds a new entry to the players HashMap if the player is not being tracked.
	 * Additionally customizes the join/leave server message format.
	 * 
	 * @param e
	 */
	@EventHandler
	public void onPlayerJoin(final PlayerQuitEvent e) {
		Player player = e.getPlayer();
		if (!AranarthUtils.getNickname(player).equals("")) {
			e.setQuitMessage(ChatUtils.translateToColor("&8[&c-&8] " + AranarthUtils.getNickname(player)));
		} else {
			e.setQuitMessage(ChatUtils.translateToColor("&8[&c-&8] &7" + AranarthUtils.getUsername(player)));
		}
	}
	
}
