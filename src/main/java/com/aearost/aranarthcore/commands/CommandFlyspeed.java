package com.aearost.aranarthcore.commands;

import com.aearost.aranarthcore.objects.AranarthPlayer;
import com.aearost.aranarthcore.utils.AranarthUtils;
import com.aearost.aranarthcore.utils.ChatUtils;
import org.bukkit.command.CommandSender;
import org.bukkit.entity.Player;

/**
 * Allows a player to modify the speed of their flight.
 */
public class CommandFlyspeed {

	/**
	 * @param sender The user that entered the command.
	 * @param args The arguments of the command.
	 */
	public static boolean onCommand(CommandSender sender, String[] args) {
		if (sender instanceof Player player) {
			AranarthPlayer aranarthPlayer = AranarthUtils.getPlayer(player.getUniqueId());
			if (aranarthPlayer.getCouncilRank() == 0) {
				player.sendMessage(ChatUtils.chatMessage("&cYou do not have permission to use this command!"));
				return false;
			}

			if (args.length < 2) {
				player.sendMessage(ChatUtils.chatMessage("&cYou must enter a speed!"));
				return true;
			}

			float speed = 0;
			try {
				speed = Float.parseFloat(args[1]);
				if (speed < 1) {
					throw new NumberFormatException();
				}
				if (speed > 10) {
					throw new NumberFormatException();
				}
				speed = speed / 10;
			} catch (NumberFormatException e) {
				player.sendMessage(ChatUtils.chatMessage("&cThe speed must be between 1 and 10!"));
				return true;
			}
			player.setFlySpeed(speed);
			player.sendMessage(ChatUtils.chatMessage("&7You have updated your flying speed"));
			return true;
		} else {
			sender.sendMessage(ChatUtils.chatMessage("&cYou must be a player to execute this command!"));
			return true;
		}
	}

}
