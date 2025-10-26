package com.aearost.aranarthcore.event.listener.misc;

import com.aearost.aranarthcore.AranarthCore;
import com.aearost.aranarthcore.enums.SpecialDay;
import com.aearost.aranarthcore.objects.AranarthPlayer;
import com.aearost.aranarthcore.utils.AranarthUtils;
import com.aearost.aranarthcore.utils.ChatUtils;
import com.aearost.aranarthcore.utils.DateUtils;
import com.aearost.aranarthcore.utils.PermissionUtils;
import org.bukkit.Bukkit;
import org.bukkit.Sound;
import org.bukkit.entity.Player;
import org.bukkit.event.EventHandler;
import org.bukkit.event.Listener;
import org.bukkit.event.player.PlayerJoinEvent;
import org.bukkit.scheduler.BukkitRunnable;

/**
 * Adds a new entry to the players HashMap if the player is not being tracked.
 * Additionally, customizes the join/leave server message format.
 */
public class PlayerServerJoinListener implements Listener {

	public PlayerServerJoinListener(AranarthCore plugin) {
		Bukkit.getPluginManager().registerEvents(this, plugin);
	}

	@EventHandler
	public void onPlayerJoin(final PlayerJoinEvent e) {
		Player player = e.getPlayer();
		if (!AranarthUtils.hasPlayedBefore(player)) {
			AranarthUtils.addPlayer(player.getUniqueId(), new AranarthPlayer(player.getName()));
		}
		// If the player changed their username
		else if (AranarthUtils.getUsername(player) != null && !AranarthUtils.getUsername(player).equals(player.getName())) {
			AranarthUtils.setUsername(player);
		}
		DateUtils dateUtils = new DateUtils();
		String nameToDisplay;
		
		if (!AranarthUtils.getNickname(player).isEmpty()) {
			nameToDisplay = "&e" + AranarthUtils.getNickname(player);
		} else {
			nameToDisplay = "&e" + AranarthUtils.getUsername(player);
		}
		
		if (dateUtils.isValentinesDay()) {
			e.setJoinMessage(ChatUtils.translateToColor("&8[&a+&8] &7" + ChatUtils.getSpecialJoinMessage(nameToDisplay, SpecialDay.VALENTINES)));
		} else if (dateUtils.isEaster()) {
			e.setJoinMessage(ChatUtils.translateToColor("&8[&a+&8] &7" + ChatUtils.getSpecialJoinMessage(nameToDisplay, SpecialDay.EASTER)));
		} else if (dateUtils.isHalloween()) {
			e.setJoinMessage(ChatUtils.translateToColor("&8[&a+&8] &7" + ChatUtils.getSpecialJoinMessage(nameToDisplay, SpecialDay.HALLOWEEN)));
		} else if (dateUtils.isChristmas()) {
			e.setJoinMessage(ChatUtils.translateToColor("&8[&a+&8] &7" + ChatUtils.getSpecialJoinMessage(nameToDisplay, SpecialDay.CHRISTMAS)));
		} else {
			e.setJoinMessage(ChatUtils.translateToColor("&8[&a+&8] &7" + nameToDisplay));
		}

		PermissionUtils.evaluatePlayerPermissions(player);
		playJoinSound();
	}

	/**
	 * Plays a sound effect when a player joins the server.
	 */
	private void playJoinSound() {
		new BukkitRunnable() {
			int runs = 0;
			@Override
			public void run() {
				if (runs == 0) {
					for (Player onlinePlayer : Bukkit.getOnlinePlayers()) {
						onlinePlayer.playSound(onlinePlayer.getLocation(), Sound.BLOCK_NOTE_BLOCK_IRON_XYLOPHONE, 1F, 1F);
					}
					runs++;
				} else if (runs == 1){
					for (Player onlinePlayer : Bukkit.getOnlinePlayers()) {
						onlinePlayer.playSound(onlinePlayer.getLocation(), Sound.BLOCK_NOTE_BLOCK_IRON_XYLOPHONE, 1F, 1.2F);
					}
					runs++;
				} else {
					for (Player onlinePlayer : Bukkit.getOnlinePlayers()) {
						onlinePlayer.playSound(onlinePlayer.getLocation(), Sound.BLOCK_NOTE_BLOCK_IRON_XYLOPHONE, 1F, 1.6F);
					}
					cancel();
				}
			}
		}.runTaskTimer(AranarthCore.getInstance(), 0, 5); // Runs every 5 ticks
	}


}
