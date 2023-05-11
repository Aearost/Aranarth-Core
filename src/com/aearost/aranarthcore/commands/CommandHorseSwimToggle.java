package com.aearost.aranarthcore.commands;

import org.bukkit.command.Command;
import org.bukkit.command.CommandSender;
import org.bukkit.entity.Horse;
import org.bukkit.entity.Player;

import com.aearost.aranarthcore.objects.AranarthPlayer;
import com.aearost.aranarthcore.utils.AranarthUtils;
import com.aearost.aranarthcore.utils.ChatUtils;

public class CommandHorseSwimToggle {

	public static boolean onCommand(CommandSender sender, Command cmd, String label, String[] args) {
		if (sender instanceof Player) {
			Player player = (Player) sender;
			if (player.isInsideVehicle() && player.getVehicle() instanceof Horse) {
				AranarthPlayer aranarthPlayer = AranarthUtils.getPlayer(player.getUniqueId());
				if (aranarthPlayer.getIsHorseSwimEnabled()) {
					player.sendMessage(ChatUtils.chatMessage("&7Your horse will no longer swim."));
					aranarthPlayer.setIsHorseSwimEnabled(false);
				} else {
					player.sendMessage(ChatUtils.chatMessage("&aYour horse will now swim!"));
					aranarthPlayer.setIsHorseSwimEnabled(true);
				}
				AranarthUtils.setPlayer(player, aranarthPlayer);
				return true;
			} else {
				player.sendMessage(ChatUtils.chatMessageError("You must be on a horse to run this command!"));
			}
		} else {
			sender.sendMessage(ChatUtils.chatMessageError("You must be a player to execute this command!"));
		}
		return false;
	}

}
