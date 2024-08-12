package com.aearost.aranarthcore.commands;

import org.bukkit.Bukkit;
import org.bukkit.Location;
import org.bukkit.command.CommandSender;
import org.bukkit.entity.Player;

import com.aearost.aranarthcore.utils.ChatUtils;

/**
 * Allows players to view the current location of another player.
 */
public class CommandWhereIs {

	/**
	 * @param sender The user that entered the command.
	 * @param args The arguments of the command.
	 * @return Confirmation of whether the command was a success or not.
	 */
	public static boolean onCommand(CommandSender sender, String[] args) {
		if (args.length == 1) {
			sender.sendMessage(ChatUtils.chatMessage("&cYou must enter a player's username!"));
		} else {
			boolean isPlayerFound = false;
			for (Player onlinePlayer : Bukkit.getOnlinePlayers()) {
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
			}
		}
		return false;
	}

}
