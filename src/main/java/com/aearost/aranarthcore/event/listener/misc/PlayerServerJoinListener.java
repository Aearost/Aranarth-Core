package com.aearost.aranarthcore.event.listener.misc;

import com.aearost.aranarthcore.AranarthCore;
import com.aearost.aranarthcore.enums.SpecialDay;
import com.aearost.aranarthcore.objects.AranarthPlayer;
import com.aearost.aranarthcore.objects.Avatar;
import com.aearost.aranarthcore.utils.*;
import com.projectkorra.projectkorra.BendingPlayer;
import org.bukkit.Bukkit;
import org.bukkit.Location;
import org.bukkit.Sound;
import org.bukkit.entity.Player;
import org.bukkit.event.EventHandler;
import org.bukkit.event.Listener;
import org.bukkit.event.player.PlayerJoinEvent;
import org.bukkit.scheduler.BukkitRunnable;

import java.text.NumberFormat;
import java.util.HashMap;

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

		boolean isNewPlayer = false;
		if (!AranarthUtils.hasPlayedBefore(player)) {
			AranarthUtils.addPlayer(player.getUniqueId(), new AranarthPlayer(player.getName()));
			player.teleport(new Location(Bukkit.getWorld("world"), -29.5, 74, -73.5, 0, 0));
			isNewPlayer = true;
		}
		// If the player changed their username
		else if (AranarthUtils.getUsername(player) != null && !AranarthUtils.getUsername(player).equals(player.getName())) {
			AranarthUtils.setUsername(player);
		}

		if (AvatarUtils.getCurrentAvatar() != null) {
			// Called to bind the Avatar's abilities to prevent loss of avatar abilities
			if (AvatarUtils.getCurrentAvatar().getUuid().equals(player.getUniqueId())) {
				PersistenceUtils.loadAvatarBinds();

				// Adds a 2-second delay
				new BukkitRunnable() {
					@Override
					public void run() {
						BendingPlayer bendingPlayer = BendingPlayer.getBendingPlayer(player);
						if (bendingPlayer != null) {
							player.performCommand("b board");
							player.performCommand("b board");
						}
					}
				}.runTaskLater(AranarthCore.getInstance(), 30);
			}
		}

		// Permissions must be applied before nickname check is done
		PermissionUtils.evaluatePlayerPermissions(player, false);

		// Clears a player's nickname if they do not have permission for one
		if (!player.hasPermission("aranarth.nick")) {
			if (!AranarthUtils.getNickname(player).equals(player.getName())) {
				AranarthPlayer aranarthPlayer = AranarthUtils.getPlayer(player.getUniqueId());
				aranarthPlayer.setNickname("");
				AranarthUtils.setPlayer(player.getUniqueId(), aranarthPlayer);
				player.sendMessage(ChatUtils.chatMessage("&7Your nickname has been cleared"));
			}
		}

		if (!isNewPlayer) {
			displayMotd(player);
		}

		DateUtils dateUtils = new DateUtils();
		String nameToDisplay;
		
		if (!AranarthUtils.getNickname(player).isEmpty()) {
			nameToDisplay = "&7" + AranarthUtils.getNickname(player);
		} else {
			nameToDisplay = "&7" + AranarthUtils.getUsername(player);
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

		boolean finalIsNewPlayer = isNewPlayer;
		new BukkitRunnable() {
			@Override
			public void run() {
				// Displays a welcome message after the join message
				if (finalIsNewPlayer) {
					Bukkit.broadcastMessage("");
					Bukkit.broadcastMessage(ChatUtils.translateToColor("                &6&l-------------------------"));
					Bukkit.broadcastMessage(ChatUtils.translateToColor("                     &7Welcome, &e" + player.getName() + ","
							+ "\n                    &7to the &6&lRealm of Aranarth!"));
					Bukkit.broadcastMessage(ChatUtils.translateToColor("                &6&l-------------------------"));
					Bukkit.broadcastMessage("");
				}
			}
		}.runTaskLater(AranarthCore.getInstance(), 1L);

		playJoinSound();
	}

	/**
	 * Displays the MOTD to the player when they join.
	 * @param player The player.
	 */
	private void displayMotd(Player player) {
		// Displays the MOTD when the player joins
		int day = AranarthUtils.getDay();
		String weekday = DateUtils.provideWeekdayName(AranarthUtils.getWeekday());
		String month = DateUtils.provideMonthName(AranarthUtils.getMonth());
		int year = AranarthUtils.getYear();

		String[] messages = DateUtils.determineServerDate(day, weekday, month, year);
		player.sendMessage(ChatUtils.translateToColor("&6&l-------------------------------------"));

		Avatar avatar = AvatarUtils.getCurrentAvatar();
		if (avatar == null) {
			player.sendMessage(ChatUtils.translateToColor("  &7&oAranarth is currently without an Avatar..."));
		} else {
			String avatarNickname = AranarthUtils.getPlayer(avatar.getUuid()).getNickname();
			player.sendMessage(ChatUtils.translateToColor("  &5&lThe current Avatar is &e" + avatarNickname));
		}

		AranarthPlayer aranarthPlayer = AranarthUtils.getPlayer(player.getUniqueId());
		NumberFormat formatter = NumberFormat.getCurrencyInstance();

		player.sendMessage(ChatUtils.translateToColor("  &7&oYour balance is currently &6" + formatter.format(aranarthPlayer.getBalance())));
		HashMap<String, String> activeBoosts = AranarthUtils.getActiveServerBoostsMessages();
		if (activeBoosts.isEmpty()) {
			player.sendMessage(ChatUtils.translateToColor("  &7There are currently no active server boosts"));
		} else {
			for (String boost : activeBoosts.keySet()) {
				player.sendMessage(ChatUtils.translateToColor("  " + boost + " &7is active for &e" + activeBoosts.get(boost)));
			}
		}

		player.sendMessage("  " + messages[1]); // Date message

		// Once mail is added in, use the below format
//		player.sendMessage(ChatUtils.chatMessage("&7You have &e" + aranarthPlayer.getMail() + " &7messages in your mail!"));

		player.sendMessage(ChatUtils.translateToColor("&6&l-------------------------------------"));
		player.sendMessage("");
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
