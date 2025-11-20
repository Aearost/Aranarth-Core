package com.aearost.aranarthcore.commands;

import com.aearost.aranarthcore.utils.ChatUtils;
import org.bukkit.Bukkit;
import org.bukkit.Location;
import org.bukkit.command.CommandSender;
import org.bukkit.entity.Player;

/**
 * Allows specified players to view the current location of another player.
 */
public class CommandWhereIs {

	/**
	 * @param sender The user that entered the command.
	 * @param args The arguments of the command.
	 */
	public static boolean onCommand(CommandSender sender, String[] args) {
		if (sender instanceof Player player) {
			if (!player.hasPermission("aranarth.whereis")) {
				player.sendMessage(ChatUtils.chatMessage("&cYou do not have permission to execute this command!"));
				return true;
			}
		}
		Bukkit.getLogger().info("A");

		if (args.length == 1) {
			sender.sendMessage(ChatUtils.chatMessage("&cYou must enter a player's username!"));
			return true;
		} else {
			Bukkit.getLogger().info("B");
			boolean isPlayerFound = false;
			for (Player onlinePlayer : Bukkit.getOnlinePlayers()) {
				Bukkit.getLogger().info("C");
				if (args[1].equalsIgnoreCase(onlinePlayer.getName())) {
					Location location = onlinePlayer.getLocation();
					sender.sendMessage(ChatUtils.chatMessage(onlinePlayer.getDisplayName()
							+ " &7is in &e" + location.getWorld().getName() + " &7at &ex: " + location.getBlockX() + " | y: " + location.getBlockY() +
							" | z: " + location.getBlockZ()));
					return true;
				}
			}
			if (!isPlayerFound) {
				sender.sendMessage(ChatUtils.chatMessage("&cThat player is not online!"));
				return true;
			}
		}
		return false;
	}

}
