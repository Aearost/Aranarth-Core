package com.aearost.aranarthcore.commands;

import org.bukkit.command.CommandSender;
import org.bukkit.entity.Camel;
import org.bukkit.entity.Horse;
import org.bukkit.entity.Player;

import com.aearost.aranarthcore.objects.AranarthPlayer;
import com.aearost.aranarthcore.utils.AranarthUtils;
import com.aearost.aranarthcore.utils.ChatUtils;

/**
 * Allows horses and camels to swim and float in water.
 */
public class CommandMountSwimToggle {

	/**
	 * @param sender The user that entered the command.
	 * @return Confirmation of whether the command was a success or not.
	 */
	public static boolean onCommand(CommandSender sender) {
		if (sender instanceof Player player) {
            if (player.isInsideVehicle() && (player.getVehicle() instanceof Horse || player.getVehicle() instanceof Camel)) {
				AranarthPlayer aranarthPlayer = AranarthUtils.getPlayer(player.getUniqueId());
				if (aranarthPlayer.getIsMountSwimEnabled()) {
					player.sendMessage(ChatUtils.chatMessage("&7Your mount will no longer swim."));
					aranarthPlayer.setIsMountSwimEnabled(false);
				} else {
					player.sendMessage(ChatUtils.chatMessage("&aYour mount will now swim!"));
					aranarthPlayer.setIsMountSwimEnabled(true);
				}
				AranarthUtils.setPlayer(player.getUniqueId(), aranarthPlayer);
				return true;
			} else {
				player.sendMessage(ChatUtils.chatMessage("&cYou must be on a mount to run this command!"));
			}
		} else {
			sender.sendMessage(ChatUtils.chatMessage("&cYou must be a player to execute this command!"));
		}
		return false;
	}

}
