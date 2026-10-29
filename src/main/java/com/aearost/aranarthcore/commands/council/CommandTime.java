package com.aearost.aranarthcore.commands.council;

import com.aearost.aranarthcore.network.NetworkManager;
import com.aearost.aranarthcore.utils.AranarthUtils;
import com.aearost.aranarthcore.utils.ChatUtils;
import com.aearost.aranarthcore.utils.Lang;
import org.bukkit.Bukkit;
import org.bukkit.World;
import org.bukkit.command.CommandSender;
import org.bukkit.entity.Player;

import java.util.List;

/**
 * Updates the time across all survival worlds.
 */
public class CommandTime {

	/**
	 * @param sender The user that entered the command.
	 * @param args The arguments of the command.
	 */
	public static boolean onCommand(CommandSender sender, String[] args) {
		if (sender instanceof Player player) {
			if (!player.hasPermission("aranarth.time")) {
				player.sendMessage(ChatUtils.chatMessage(Lang.getFor(player, "general.no_permission")));
				return true;
			}
		}

		if (args.length == 1) {
			World survival = Bukkit.getWorld("world");
			long ticks = survival != null ? survival.getTime() : 0;
			sender.sendMessage(ChatUtils.chatMessage(Lang.getFor(sender, "admin.time_current", "ticks", String.valueOf(ticks), "label", getTimeOfDay(sender, ticks))));
			return true;
		}

		try {
			long time = Long.parseLong(args[1]);
			updateTime(time);
			sender.sendMessage(ChatUtils.chatMessage(Lang.getFor(sender, "admin.time_updated")));
		} catch (NumberFormatException e) {
			long time = -1;
			if (args[1].equalsIgnoreCase("day")) {
				time = 0;
			} else if (args[1].equalsIgnoreCase("noon")) {
				time = 6000;
			} else if (args[1].equalsIgnoreCase("night")) {
				time = 13000;
			} else if (args[1].equalsIgnoreCase("midnight")) {
				time = 18000;
			}

			if (time == -1) {
				sender.sendMessage(ChatUtils.chatMessage(Lang.getFor(sender, "general.invalid_syntax", "usage", "ac time <time>")));
			} else {
				updateTime(time);
				sender.sendMessage(ChatUtils.chatMessage(Lang.getFor(sender, "admin.time_updated")));
			}
		}
		return true;
	}

	/**
	 * Returns an approximate time-of-day label for the given tick value.
	 * Minecraft day starts at tick 0 (dawn). One full day = 24000 ticks.
	 * @param sender The sender the label is shown to.
	 * @param ticks The world time in ticks.
	 * @return A human-readable time-of-day label.
	 */
	private static String getTimeOfDay(CommandSender sender, long ticks) {
		long t = ticks % 24000;
		String key;
		if (t < 1000) key = "admin.time_dawn";
		else if (t < 3000) key = "admin.time_morning";
		else if (t < 6000) key = "admin.time_late_morning";
		else if (t < 7000) key = "admin.time_noon";
		else if (t < 9000) key = "admin.time_afternoon";
		else if (t < 11000) key = "admin.time_late_afternoon";
		else if (t < 12000) key = "admin.time_dusk";
		else if (t < 13000) key = "admin.time_evening";
		else if (t < 18000) key = "admin.time_night";
		else if (t < 22000) key = "admin.time_midnight";
		else key = "admin.time_late_night";
		return Lang.getFor(sender, key);
	}

	/**
	 * Updates the time across the three survival worlds.
	 * @param time The new time.
	 */
	private static void updateTime(long time) {
		List<World> syncWorlds = AranarthUtils.getSyncWorlds();
		for (World w : syncWorlds) {
			w.setTime(time);
		}

		// Storms are left untouched - only sleeping may end a storm early
		if (NetworkManager.isActive()) {
			NetworkManager.getInstance().publishSyncTime(time, false);
		}
	}
}
