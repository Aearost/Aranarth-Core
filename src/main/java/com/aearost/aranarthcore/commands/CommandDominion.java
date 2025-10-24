package com.aearost.aranarthcore.commands;

import com.aearost.aranarthcore.objects.Dominion;
import com.aearost.aranarthcore.utils.ChatUtils;
import com.aearost.aranarthcore.utils.DominionUtils;
import org.bukkit.command.CommandSender;
import org.bukkit.entity.Player;

/**
 * Centralizes all functionality relating to dominions.
 */
public class CommandDominion {

	/**
	 * @param sender The user that entered the command.
	 * @param args The arguments of the command.
	 * @return Confirmation of whether the command was a success or not.
	 */
	public static boolean onCommand(CommandSender sender, String[] args) {
		// Shorthand of /ac dominion home
		if (args.length == 1) {
			if (sender instanceof Player player) {
				teleportToDominionHome(player);
			} else {
				sender.sendMessage(ChatUtils.chatMessage("&cYou must be a player to execute this command!"));
				return true;
			}
		} else {
			if (args.length >= 2) {
				if (sender instanceof Player player) {
	//				/ac dominion create
					if (args[1].equalsIgnoreCase("create")) {

					}
//					/ac dominion add
					else if (args[1].equalsIgnoreCase("add")) {

					}
//					/ac dominion remove
					else if (args[1].equalsIgnoreCase("remove")) {

					}
//					/ac dominion disband
					else if (args[1].equalsIgnoreCase("disband")) {

					}
//					/ac dominion claim
					else if (args[1].equalsIgnoreCase("claim")) {

					}
//					/ac dominion unclaim
					else if (args[1].equalsIgnoreCase("unclaim")) {

					}
//					/ac dominion balance
					else if (args[1].equalsIgnoreCase("balance")) {

					}
				} else {
					sender.sendMessage(ChatUtils.chatMessage("&cYou must be a player to execute this command!"));
					return true;
				}
			}
		}
		return false;
	}

	/**
	 * Teleports the player to their dominion's home.
	 * @param player The player.
	 */
	private static void teleportToDominionHome(Player player) {
		Dominion dominion = DominionUtils.getPlayerDominion(player);
		if (dominion != null) {
			player.teleport(dominion.getDominionHome());
			player.sendMessage(ChatUtils.chatMessage("&7You have teleported to your dominion"));
		} else {
			player.sendMessage(ChatUtils.chatMessage("&cYou are not in a dominion!"));
		}
	}

}
