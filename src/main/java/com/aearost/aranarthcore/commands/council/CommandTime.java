package com.aearost.aranarthcore.commands.council;

import com.aearost.aranarthcore.enums.Weather;
import com.aearost.aranarthcore.network.NetworkManager;
import com.aearost.aranarthcore.utils.AranarthUtils;
import com.aearost.aranarthcore.utils.ChatUtils;
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
				player.sendMessage(ChatUtils.chatMessage("&cYou cannot use this command!"));
				return true;
			}
		}

		if (args.length == 1) {
			World survival = Bukkit.getWorld("world");
			long ticks = survival != null ? survival.getTime() : 0;
			sender.sendMessage(ChatUtils.chatMessage("&7Current time: &e" + ticks + " ticks &7(" + getTimeOfDay(ticks) + ")"));
			return true;
		}

		try {
			long time = Long.parseLong(args[1]);
			updateTime(time);
			sender.sendMessage(ChatUtils.chatMessage("&7The time has been updated"));
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
				sender.sendMessage(ChatUtils.chatMessage("&cInvalid syntax: &e/ac time <time>"));
			} else {
				updateTime(time);
				sender.sendMessage(ChatUtils.chatMessage("&7The time has been updated"));
			}
		}
		return true;
	}

	/**
	 * Returns an approximate time-of-day label for the given tick value.
	 * Minecraft day starts at tick 0 (dawn). One full day = 24000 ticks.
	 * @param ticks The world time in ticks.
	 * @return A human-readable time-of-day label.
	 */
	private static String getTimeOfDay(long ticks) {
		long t = ticks % 24000;
		if (t < 1000) return "Dawn";
		if (t < 3000) return "Morning";
		if (t < 6000) return "Late Morning";
		if (t < 7000) return "Noon";
		if (t < 9000) return "Afternoon";
		if (t < 11000) return "Late Afternoon";
		if (t < 12000) return "Dusk";
		if (t < 13000) return "Evening";
		if (t < 18000) return "Night";
		if (t < 22000) return "Midnight";
		return "Late Night";
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

		// Immediately end any storm, will be picked up by DateUtils logic within 5 seconds
		if (AranarthUtils.getWeather() != Weather.CLEAR) {
			AranarthUtils.setStormDuration(0);
			for (World w : syncWorlds) {
				w.setThunderDuration(0);
				w.setWeatherDuration(0);
			}
		}

		if (NetworkManager.isActive()) {
			NetworkManager.getInstance().publishSyncTime(time);
		}
	}
}
