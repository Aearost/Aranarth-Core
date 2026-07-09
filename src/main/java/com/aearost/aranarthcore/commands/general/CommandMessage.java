package com.aearost.aranarthcore.commands.general;

import com.aearost.aranarthcore.objects.AranarthPlayer;
import com.aearost.aranarthcore.utils.AranarthUtils;
import com.aearost.aranarthcore.utils.ChatUtils;
import org.bukkit.Bukkit;
import org.bukkit.command.Command;
import org.bukkit.command.CommandExecutor;
import org.bukkit.command.CommandSender;
import org.bukkit.entity.Player;

import java.util.UUID;

/**
 * Allows a player to send a private message to another player.
 */
public class CommandMessage implements CommandExecutor {

	/**
	 * @param sender The user that entered the command.
	 * @param command The command itself.
	 * @param alias The alias of the command.
	 * @param args The arguments of the command.
	 * @return Confirmation of whether the command was a success or not.
	 */
	@Override
	public boolean onCommand(CommandSender sender, Command command, String alias, String[] args) {
		if (sender instanceof Player player) {
			if (args.length <= 1) {
				player.sendMessage(ChatUtils.chatMessage("&cInvalid syntax: &e/msg <player> <message>"));
				return true;
			} else {
				UUID targetUuid = AranarthUtils.getUUIDFromUsernameOrNickname(args[0]);
				// If the player has played before
				if (targetUuid != null) {
					Player target = Bukkit.getPlayer(targetUuid);
					AranarthPlayer aranarthPlayer = AranarthUtils.getPlayer(player.getUniqueId());
					AranarthPlayer targetAranarthPlayer = AranarthUtils.getPlayer(targetUuid);
					if (targetAranarthPlayer.isTogglingMessages() && aranarthPlayer.getCouncilRank() == 0) {
						player.sendMessage(ChatUtils.chatMessage("&e" + targetAranarthPlayer.getNickname() + " &cis currently not receiving messages"));
						return true;
					}

					// If the player is online
					if (target != null) {
						if (ChatUtils.isPlayerMuted(player)) {
							player.sendMessage(ChatUtils.chatMessage("&cYou are muted and cannot send messages!"));
							return true;
						}
						ChatUtils.sendPrivateMessage(player, target, args, false);
						return true;
					} else {
						player.sendMessage(ChatUtils.chatMessage("&e" + targetAranarthPlayer.getNickname() + " &cis not online"));
						return true;
					}
				} else {
					player.sendMessage(ChatUtils.chatMessage("&e" + args[0] + " &ccould not be found"));
					return true;
				}
			}
		} else {
			sender.sendMessage(ChatUtils.chatMessage("&cThis command can only be used by players!"));
			return true;
		}
	}
}
