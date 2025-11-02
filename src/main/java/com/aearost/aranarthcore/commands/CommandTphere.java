package com.aearost.aranarthcore.commands;

import com.aearost.aranarthcore.objects.AranarthPlayer;
import com.aearost.aranarthcore.utils.AranarthUtils;
import com.aearost.aranarthcore.utils.ChatUtils;
import org.bukkit.Bukkit;
import org.bukkit.command.CommandSender;
import org.bukkit.entity.Player;

/**
 * Sends a request to the input player to have them teleport to the player's location.
 */
public class CommandTphere {

	/**
	 * @param sender The user that entered the command.
	 * @param args The arguments of the command.
	 */
	public static boolean onCommand(CommandSender sender, String[] args) {
		if (sender instanceof Player player) {
			if (args.length == 1) {
				sender.sendMessage(ChatUtils.chatMessage("&cYou must enter a player to teleport to!"));
				return true;
			} else {
				Player target = Bukkit.getPlayer(AranarthUtils.getUUIDFromUsername(args[1]));
				if (target != null) {
					AranarthPlayer targetPlayer = AranarthUtils.getPlayer(target.getUniqueId());
					targetPlayer.setTeleportToUuid(player.getUniqueId());
					AranarthUtils.setPlayer(target.getUniqueId(), targetPlayer);
					player.sendMessage(ChatUtils.chatMessage("&7You have sent a teleport request to &e" + targetPlayer.getNickname()));
					target.sendMessage(ChatUtils.chatMessage("&e" + targetPlayer.getNickname() + " &7has requested you teleport to them"));
					target.sendMessage(ChatUtils.chatMessage("&7Use &e/ac tpaccept &7or &e/ac tpdeny"));
				} else {
					player.sendMessage(ChatUtils.chatMessage("&cThat player is not online!"));
				}
			}
		} else {
			sender.sendMessage(ChatUtils.chatMessage("&cOnly players can execute this command!"));
		}
		return true;
	}

}
