package com.aearost.aranarthcore.commands.council;

import com.aearost.aranarthcore.network.NetworkManager;
import com.aearost.aranarthcore.network.NetworkPlayer;
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
				player.sendMessage(ChatUtils.chatMessage(Lang.getFor(player, "general.no_permission")));
				return true;
			}
		}

		if (args.length >= 3) {
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

			for (Player player : Bukkit.getOnlinePlayers()) {
				if (player.getName().equalsIgnoreCase(args[1])) {
					AranarthPlayer aranarthPlayer = AranarthUtils.getPlayer(player.getUniqueId());
					if (aranarthPlayer.getCouncilRank() >= 2) {
						sender.sendMessage(ChatUtils.chatMessage(Lang.getFor(sender, "sudo.cannot_target_player")));
						return true;
					}

					player.performCommand(command);
					sender.sendMessage(ChatUtils.chatMessage(Lang.getFor(sender, "sudo.success", "command", command, "player", aranarthPlayer.getNickname())));
					return true;
				}
			}

			// Player is not on this server - forward the command to the server they are on
			if (NetworkManager.isActive()) {
				for (NetworkPlayer np : NetworkManager.getInstance().getRemoteRoster().values()) {
					if (np.getUsername().equalsIgnoreCase(args[1])) {
						if (np.getCouncilRank() >= 2) {
							sender.sendMessage(ChatUtils.chatMessage(Lang.getFor(sender, "sudo.cannot_target_player")));
							return true;
						}

						NetworkManager.getInstance().publishSudo(np.getUuid(), command);
						String nickname = np.getNickname() != null ? np.getNickname() : np.getUsername();
						sender.sendMessage(ChatUtils.chatMessage(Lang.getFor(sender, "sudo.success", "command", command, "player", nickname)));
						return true;
					}
				}
			}
			sender.sendMessage(ChatUtils.chatMessage(Lang.getFor(sender, "player.not_found", "name", args[1])));
			return true;
		} else {
			sender.sendMessage(ChatUtils.chatMessage(Lang.getFor(sender, "general.invalid_syntax", "usage", "ac sudo <player> <command>")));
			return true;
		}
	}

}
