package com.aearost.aranarthcore.event.listener.misc;

import com.aearost.aranarthcore.AranarthCore;
import com.aearost.aranarthcore.enums.SpecialDay;
import com.aearost.aranarthcore.utils.AranarthUtils;
import com.aearost.aranarthcore.utils.ChatUtils;
import com.aearost.aranarthcore.utils.DateUtils;
import org.bukkit.Bukkit;
import org.bukkit.Sound;
import org.bukkit.entity.Player;
import org.bukkit.event.EventHandler;
import org.bukkit.event.Listener;
import org.bukkit.event.player.PlayerQuitEvent;
import org.bukkit.scheduler.BukkitRunnable;

/**
 * Adds a new entry to the players HashMap if the player is not being tracked.
 * Additionally, customizes the join/leave server message format.
 */
public class PlayerServerQuitListener implements Listener {

	public PlayerServerQuitListener(AranarthCore plugin) {
		Bukkit.getPluginManager().registerEvents(this, plugin);
	}

	@EventHandler
	public void onPlayerQuit(final PlayerQuitEvent e) {
		Player player = e.getPlayer();
		DateUtils dateUtils = new DateUtils();
		String nameToDisplay;
		if (!AranarthUtils.getNickname(player).isEmpty()) {
			nameToDisplay = "&7" + AranarthUtils.getNickname(player);
		} else {
			nameToDisplay = "&7" + AranarthUtils.getUsername(player);
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

		playQuitSound();
	}

	/**
	 * Plays a sound effect when a player quits the server.
	 */
	private void playQuitSound() {
		new BukkitRunnable() {
			int runs = 0;
			@Override
			public void run() {
				if (runs == 0) {
					for (Player onlinePlayer : Bukkit.getOnlinePlayers()) {
						onlinePlayer.playSound(onlinePlayer.getLocation(), Sound.BLOCK_NOTE_BLOCK_IRON_XYLOPHONE, 1F, 1.6F);
					}
					runs++;
				} else if (runs == 1){
					for (Player onlinePlayer : Bukkit.getOnlinePlayers()) {
						onlinePlayer.playSound(onlinePlayer.getLocation(), Sound.BLOCK_NOTE_BLOCK_IRON_XYLOPHONE, 1F, 1.2F);
					}
					runs++;
				} else {
					for (Player onlinePlayer : Bukkit.getOnlinePlayers()) {
						onlinePlayer.playSound(onlinePlayer.getLocation(), Sound.BLOCK_NOTE_BLOCK_IRON_XYLOPHONE, 1F, 0.8F);
					}
					cancel();
				}
			}
		}.runTaskTimer(AranarthCore.getInstance(), 0, 5); // Runs every 5 ticks
	}
}
