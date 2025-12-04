package com.aearost.aranarthcore.commands;

import com.aearost.aranarthcore.objects.AranarthPlayer;
import com.aearost.aranarthcore.objects.Home;
import com.aearost.aranarthcore.utils.AranarthUtils;
import com.aearost.aranarthcore.utils.ChatUtils;
import org.bukkit.Location;
import org.bukkit.Material;
import org.bukkit.command.CommandSender;
import org.bukkit.entity.Player;

/**
 * Allows the player to set a home.
 */
public class CommandSethome {

	/**
	 * @param sender The user that entered the command.
	 * @param args The arguments of the command.
	 */
	public static boolean onCommand(CommandSender sender, String[] args) {
		if (sender instanceof Player player) {
			AranarthPlayer aranarthPlayer = AranarthUtils.getPlayer(player.getUniqueId());
			int playerMaxHomeCount = AranarthUtils.getMaxHomeNum(player);
			if (aranarthPlayer.getHomes().size() < playerMaxHomeCount) {
				if (args.length >= 2) {
					Location loc = AranarthUtils.getSafeTeleportLocation(player.getLocation());
					if (loc == null) {
						player.sendMessage(ChatUtils.chatMessage("&cYou cannot set a home here!"));
						return true;
					}

					// Construct the home name from args
					StringBuilder homeNameBuilder = new StringBuilder();
					for (int i = 1; i < args.length; i++) {
						homeNameBuilder.append(args[i]);
						if (i < args.length - 1) {
							homeNameBuilder.append(" ");
						}
					}
					String homeName = homeNameBuilder.toString();

					if (player.hasPermission("aranarth.chat.hex")) {
						homeName = ChatUtils.translateToColor(homeName);
					} else if (player.hasPermission("aranarth.chat.color")) {
						homeName = ChatUtils.playerColorChat(homeName);
						if (homeName == null) {
							player.sendMessage(ChatUtils.chatMessage("&cYou cannot use this kind of formatting!"));
							return true;
						}
					}

					homeName = ChatUtils.removeSpecialCharacters(homeName);
					String strippedName = ChatUtils.stripColorFormatting(homeName);
					for (Home home : aranarthPlayer.getHomes()) {
						if (ChatUtils.stripColorFormatting(home.getName()).equalsIgnoreCase(strippedName)) {
							player.sendMessage(ChatUtils.chatMessage("&cYou cannot use the same home name twice!"));
							return true;
						}
					}

					if (strippedName.isEmpty()) {
						player.sendMessage(ChatUtils.chatMessage("&cYou must input a name!"));
						return true;
					}

					// Create the home at the computed surface location
					Home home = new Home(homeName, loc, Material.BARRIER);
					AranarthUtils.addPlayerHome(player, home);
					player.sendMessage(ChatUtils.chatMessage("&7You have added the home &e" + homeName));
					return true;
				}
			} else {
				player.sendMessage(ChatUtils.chatMessage("&cYou cannot set more than &e" + playerMaxHomeCount + " &chomes!"));
				return true;
			}
		} else {
			sender.sendMessage(ChatUtils.chatMessage("&cOnly players can execute this command!"));
			return true;
		}

		return false;
	}

}
