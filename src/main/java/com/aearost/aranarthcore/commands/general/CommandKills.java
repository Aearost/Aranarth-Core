package com.aearost.aranarthcore.commands.general;

import com.aearost.aranarthcore.network.NetworkManager;
import com.aearost.aranarthcore.network.NetworkPlayer;
import com.aearost.aranarthcore.objects.AranarthPlayer;
import com.aearost.aranarthcore.utils.AranarthUtils;
import com.aearost.aranarthcore.utils.ChatUtils;
import org.bukkit.Bukkit;
import org.bukkit.command.Command;
import org.bukkit.command.CommandExecutor;
import org.bukkit.command.CommandSender;
import org.bukkit.entity.Player;

/**
 * Provides the number of kills of the input player.
 */
public class CommandKills implements CommandExecutor {

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
			// List their own kills
			if (args.length == 0) {
				int killCount = AranarthUtils.getKillsOrDeathsInWorld(player.getUniqueId(), player.getWorld(), true);
				player.sendMessage(ChatUtils.chatMessage("&7You have &c" + killCount + " kills"));
				return true;
			} else {
				java.util.UUID targetUuid = AranarthUtils.getUUIDFromUsernameOrNickname(args[0]);
				if (targetUuid != null) {
					int killCount = AranarthUtils.getKillsOrDeathsInWorld(targetUuid, player.getWorld(), true);
					AranarthPlayer aranarthPlayer = AranarthUtils.getPlayer(targetUuid);
					String displayName;
					if (aranarthPlayer != null) {
						displayName = aranarthPlayer.getNickname();
					} else {
						NetworkPlayer remote = NetworkManager.isActive() ? NetworkManager.getInstance().getRemotePlayer(targetUuid) : null;
						if (remote != null) {
							String nick = remote.getNickname();
							displayName = (nick == null || nick.isEmpty()) ? remote.getUsername() : ChatUtils.stripColorFormatting(nick);
						} else {
							String bukkit = Bukkit.getOfflinePlayer(targetUuid).getName();
							displayName = bukkit != null ? bukkit : args[0];
						}
					}
					player.sendMessage(ChatUtils.chatMessage("&e" + displayName + " &7has &c" + killCount + " kills"));
				} else {
					player.sendMessage(ChatUtils.chatMessage("&e" + args[0] + " &ccould not be found"));
				}
				return true;
			}
		} else {
			sender.sendMessage(ChatUtils.chatMessage("&cYou must be a player to execute this command!"));
			return true;
		}
	}
}
