package com.aearost.aranarthcore.commands;

import com.aearost.aranarthcore.enums.Weather;
import com.aearost.aranarthcore.utils.AranarthUtils;
import com.aearost.aranarthcore.utils.ChatUtils;
import org.bukkit.Bukkit;
import org.bukkit.World;
import org.bukkit.command.CommandSender;
import org.bukkit.entity.Player;

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

		if (args.length >= 2) {
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
		} else {
			sender.sendMessage(ChatUtils.chatMessage("&cInvalid syntax: &e/ac time <time>"));
			return true;
		}
	}

	/**
	 * Updates the time across the three survival worlds.
	 * @param time The new time.
	 */
	private static void updateTime(long time) {
		World survival = Bukkit.getWorld("world");
		World smp = Bukkit.getWorld("smp");
		World resource = Bukkit.getWorld("resource");

		survival.setTime(time);
		smp.setTime(time);
		resource.setTime(time);

		// Immediately end any storm, will be picked up by DateUtils logic within 5 seconds
		if (AranarthUtils.getWeather() != Weather.CLEAR) {
			AranarthUtils.setStormDuration(0);
			survival.setThunderDuration(0);
			survival.setWeatherDuration(0);
			smp.setThunderDuration(0);
			smp.setWeatherDuration(0);
			resource.setThunderDuration(0);
			resource.setWeatherDuration(0);
		}
	}
}
