package com.aearost.aranarthcore.commands.council;

import com.aearost.aranarthcore.gui.GuiInvsee;
import com.aearost.aranarthcore.utils.ChatUtils;
import org.bukkit.Bukkit;
import org.bukkit.command.CommandSender;
import org.bukkit.entity.Player;

/**
 * Allows the player to see another player's inventory, including armor and off-hand slots.
 */
public class CommandInvsee {

	/**
	 * @param sender The user that entered the command.
	 * @param args The arguments of the command.
	 */
	public static boolean onCommand(CommandSender sender, String[] args) {
		if (sender instanceof Player player) {
			if (!player.hasPermission("aranarth.invsee")) {
				sender.sendMessage(ChatUtils.chatMessage("&cYou do not have permission to execute this command!"));
				return true;
			}

			if (args.length == 1) {
				player.sendMessage(ChatUtils.chatMessage("&cInvalid syntax: &e/ac invsee <player>"));
				return true;
			} else {
				Player target = Bukkit.getPlayer(args[1]);
				if (target != null) {
					GuiInvsee.open(player, target);
					return true;
				} else {
					player.sendMessage(ChatUtils.chatMessage("&cThis player could not be found"));
					return true;
				}
			}
		} else {
			sender.sendMessage(ChatUtils.chatMessage("&cYou must be a player to execute this command!"));
			return true;
		}
	}
}
