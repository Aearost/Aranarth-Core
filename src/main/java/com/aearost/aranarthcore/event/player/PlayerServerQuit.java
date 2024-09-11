package com.aearost.aranarthcore.event.player;

import com.aearost.aranarthcore.AranarthCore;
import com.aearost.aranarthcore.enums.SpecialDay;
import com.aearost.aranarthcore.utils.AranarthUtils;
import com.aearost.aranarthcore.utils.ChatUtils;
import com.aearost.aranarthcore.utils.DateUtils;
import org.bukkit.Bukkit;
import org.bukkit.entity.Player;
import org.bukkit.event.EventHandler;
import org.bukkit.event.Listener;
import org.bukkit.event.player.PlayerQuitEvent;

public class PlayerServerQuit implements Listener {

	public PlayerServerQuit(AranarthCore plugin) {
		Bukkit.getPluginManager().registerEvents(this, plugin);
	}

	/**
	 * Adds a new entry to the players HashMap if the player is not being tracked.
	 * Additionally, customizes the join/leave server message format.
	 * @param e The event.
	 */
	@EventHandler
	public void onPlayerQuit(final PlayerQuitEvent e) {
		Player player = e.getPlayer();
		DateUtils dateUtils = new DateUtils();
		String nameToDisplay;
		if (!AranarthUtils.getNickname(player).isEmpty()) {
			nameToDisplay = AranarthUtils.getNickname(player);
		} else {
			nameToDisplay = AranarthUtils.getUsername(player);
		}
		
		if (dateUtils.isValentinesDay()) {
			e.setQuitMessage(ChatUtils.translateToColor("&8[&c-&8] &7" + ChatUtils.getSpecialQuitMessage(nameToDisplay, SpecialDay.VALENTINES)));
		} else if (dateUtils.isEaster()) {
			e.setQuitMessage(ChatUtils.translateToColor("&8[&c-&8] &7" + ChatUtils.getSpecialQuitMessage(nameToDisplay, SpecialDay.EASTER)));
		} else if (dateUtils.isHalloween()) {
			e.setQuitMessage(ChatUtils.translateToColor("&8[&c-&8] &7" + ChatUtils.getSpecialQuitMessage(nameToDisplay, SpecialDay.HALLOWEEN)));
		} else if (dateUtils.isChristmas()) {
			e.setQuitMessage(ChatUtils.translateToColor("&8[&c-&8] &7" + ChatUtils.getSpecialQuitMessage(nameToDisplay, SpecialDay.CHRISTMAS)));
		} else {
			e.setQuitMessage(ChatUtils.translateToColor("&8[&c-&8] &7" + nameToDisplay));
		}
		
	}
	
}
