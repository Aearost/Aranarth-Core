package com.aearost.aranarthcore.event.listener.misc;

import com.aearost.aranarthcore.AranarthCore;
import com.aearost.aranarthcore.database.DatabaseManager;
import com.aearost.aranarthcore.enums.SpecialDay;
import com.aearost.aranarthcore.network.NetworkManager;
import com.aearost.aranarthcore.objects.AranarthPlayer;
import com.aearost.aranarthcore.utils.*;
import org.bukkit.Bukkit;
import org.bukkit.Sound;
import org.bukkit.entity.Player;
import org.bukkit.event.EventHandler;
import org.bukkit.event.Listener;
import org.bukkit.event.player.PlayerQuitEvent;
import org.bukkit.scheduler.BukkitRunnable;

import java.util.HashMap;

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

		boolean isCrossServerTransfer = NetworkManager.isActive()
				&& NetworkManager.getInstance().consumeTransferring(player.getUniqueId());

		// Persist the player's logout location so they are returned here on next login.
		// Skipped for cross-server transfers; the receiving server will track their location.
		if (!isCrossServerTransfer && DatabaseManager.isActive()) {
			final String server = NetworkManager.isActive() ? NetworkManager.getInstance().getThisServer() : "survival";
			final org.bukkit.Location loc = player.getLocation();
			final String world = loc.getWorld().getName();
			final double x = loc.getX(), y = loc.getY(), z = loc.getZ();
			final float yaw = loc.getYaw(), pitch = loc.getPitch();
			Bukkit.getScheduler().runTaskAsynchronously(AranarthCore.getInstance(), () ->
				DatabaseManager.getInstance().saveLastLocation(player.getUniqueId(), server, world, x, y, z, yaw, pitch)
			);
		}

		if (NetworkManager.isActive()) {
			// Pass the formatted quit message so the other server can display it.
			// Cross-server transfers suppress the message (null → empty string in the JSON).
			AranarthPlayer apForQuit = AranarthUtils.getPlayer(player.getUniqueId());
			boolean isVanished = apForQuit != null && apForQuit.isVanished();
			String crossServerQuitMsg = null;
			if (!isCrossServerTransfer && !isVanished) {
				DateUtils dateUtils = new DateUtils();
				String nameToDisplay = "&7" + AranarthUtils.getNickname(player);
				if (dateUtils.isValentinesDay()) {
					crossServerQuitMsg = ChatUtils.translateToColor("&8[&c-&8] &7" + ChatUtils.getSpecialQuitMessage(nameToDisplay, SpecialDay.VALENTINES));
				} else if (dateUtils.isEaster()) {
					crossServerQuitMsg = ChatUtils.translateToColor("&8[&c-&8] &7" + ChatUtils.getSpecialQuitMessage(nameToDisplay, SpecialDay.EASTER));
				} else if (dateUtils.isHalloween()) {
					crossServerQuitMsg = ChatUtils.translateToColor("&8[&c-&8] &7" + ChatUtils.getSpecialQuitMessage(nameToDisplay, SpecialDay.HALLOWEEN));
				} else if (dateUtils.isChristmas()) {
					crossServerQuitMsg = ChatUtils.translateToColor("&8[&c-&8] &7" + ChatUtils.getSpecialQuitMessage(nameToDisplay, SpecialDay.CHRISTMAS));
				} else {
					crossServerQuitMsg = ChatUtils.translateToColor("&8[&c-&8] &7" + nameToDisplay);
				}
			}
			NetworkManager.getInstance().publishPlayerQuit(player.getUniqueId(), crossServerQuitMsg, isVanished);
		}
		PersistenceUtils.saveQuestProgress();
		PersistenceUtils.saveLoginStreaks();
		PermissionUtils.clearPlayerAttachments(player.getUniqueId());
		AranarthPlayer aranarthPlayer = AranarthUtils.getPlayer(player.getUniqueId());
		aranarthPlayer.setAfkLocation(null);
		if (!aranarthPlayer.getCombatLogTime().isEmpty()) {
			player.setHealth(0);
		}

		aranarthPlayer.setCombatLogTime(new HashMap<>());
		AranarthUtils.setPlayer(player.getUniqueId(), aranarthPlayer);

		// Called to save the Avatar's abilities to prevent loss of avatar abilities
		if (AvatarUtils.getCurrentAvatar() != null) {
			if (AvatarUtils.getCurrentAvatar().getUuid().equals(player.getUniqueId())) {
				PersistenceUtils.saveAvatarBinds();
			}
		}

		if (isCrossServerTransfer) {
			e.setQuitMessage(null);
			// Suppress DiscordSRV leave announcement for server-switch departures
			if (NetworkManager.isActive()) {
				NetworkManager.getInstance().markCrossServerQuit(player.getUniqueId());
			}
		} else {
			DateUtils dateUtils = new DateUtils();
			String nameToDisplay;
			nameToDisplay = "&7" + AranarthUtils.getNickname(player);

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

		AranarthUtils.updateTab();
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
						onlinePlayer.playSound(onlinePlayer, Sound.BLOCK_NOTE_BLOCK_IRON_XYLOPHONE, 1F, 1.6F);
					}
					runs++;
				} else if (runs == 1){
					for (Player onlinePlayer : Bukkit.getOnlinePlayers()) {
						onlinePlayer.playSound(onlinePlayer, Sound.BLOCK_NOTE_BLOCK_IRON_XYLOPHONE, 1F, 1.2F);
					}
					runs++;
				} else {
					for (Player onlinePlayer : Bukkit.getOnlinePlayers()) {
						onlinePlayer.playSound(onlinePlayer, Sound.BLOCK_NOTE_BLOCK_IRON_XYLOPHONE, 1F, 0.8F);
					}
					cancel();
				}
			}
		}.runTaskTimer(AranarthCore.getInstance(), 0, 5); // Runs every 5 ticks
	}
}
