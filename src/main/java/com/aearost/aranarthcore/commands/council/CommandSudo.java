package com.aearost.aranarthcore.commands.council;

import com.aearost.aranarthcore.objects.AranarthPlayer;
import com.aearost.aranarthcore.utils.AranarthUtils;
import com.aearost.aranarthcore.utils.ChatUtils;
import com.aearost.aranarthcore.utils.Lang;
import org.bukkit.Bukkit;
import org.bukkit.command.CommandSender;
import org.bukkit.entity.Player;

/**
 * Runs a command on behalf of the input player.
 */
public class CommandSudo {

	/**
	 * @param sender The user that entered the command.
	 * @param args The arguments of the command.
	 */
	public static boolean onCommand(CommandSender sender, String[] args) {
		if (sender instanceof Player player) {
			if (!player.hasPermission("aranarth.sudo")) {
				player.sendMessage(ChatUtils.chatMessage(Lang.get("general.no_permission")));
				return true;
			}
		}

		if (args.length >= 3) {
			for (Player player : Bukkit.getOnlinePlayers()) {
				if (player.getName().equalsIgnoreCase(args[1])) {
					AranarthPlayer aranarthPlayer = AranarthUtils.getPlayer(player.getUniqueId());
					if (aranarthPlayer.getCouncilRank() >= 2) {
						sender.sendMessage(ChatUtils.chatMessage(Lang.get("sudo.cannot_target_player")));
						return true;
					}

					String command = "";
					for (int i = 2; i < args.length; i++) {
						command += args[i];
						if (i == 2 && args[i].startsWith("/")) {
							command = command.replaceAll("/", "");
						}

						if (i < args.length - 1) {
							command += " ";
						}
					}

					player.performCommand(command);
					sender.sendMessage(ChatUtils.chatMessage(Lang.get("sudo.success", "command", command, "player", aranarthPlayer.getNickname())));
					return true;
				}
			}
			sender.sendMessage(ChatUtils.chatMessage(Lang.get("player.not_found", "name", args[1])));
			return true;
		} else {
			sender.sendMessage(ChatUtils.chatMessage(Lang.get("general.invalid_syntax", "usage", "ac sudo <player> <command>")));
			return true;
		}
	}

}
