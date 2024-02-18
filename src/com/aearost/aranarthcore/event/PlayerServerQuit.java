package com.aearost.aranarthcore.event;

import java.time.LocalDate;

import org.bukkit.Bukkit;
import org.bukkit.entity.Player;
import org.bukkit.event.EventHandler;
import org.bukkit.event.Listener;
import org.bukkit.event.player.PlayerQuitEvent;

import com.aearost.aranarthcore.AranarthCore;
import com.aearost.aranarthcore.utils.AranarthUtils;
import com.aearost.aranarthcore.utils.ChatUtils;

public class PlayerServerQuit implements Listener {

	public PlayerServerQuit(AranarthCore plugin) {
		Bukkit.getPluginManager().registerEvents(this, plugin);
	}

	/**
	 * Adds a new entry to the players HashMap if the player is not being tracked.
	 * Additionally customizes the join/leave server message format.
	 * 
	 * @param e
	 */
	@EventHandler
	public void onPlayerQuit(final PlayerQuitEvent e) {
		Player player = e.getPlayer();
		
		int month = LocalDate.now().getMonthValue();
		int day = LocalDate.now().getDayOfMonth();
		System.out.println("Month: " + month);
		System.out.println("Day: " + day);
		
		if (!AranarthUtils.getNickname(player).equals("")) {
			e.setQuitMessage(ChatUtils.translateToColor("&8[&c-&8] " + AranarthUtils.getNickname(player)));
		} else {
			e.setQuitMessage(ChatUtils.translateToColor("&8[&c-&8] &7" + AranarthUtils.getUsername(player)));
		}
	}
	
}