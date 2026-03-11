package com.aearost.aranarthcore.commands.general;

import com.aearost.aranarthcore.objects.AranarthPlayer;
import com.aearost.aranarthcore.utils.AranarthUtils;
import com.aearost.aranarthcore.utils.ChatUtils;
import org.bukkit.Bukkit;
import org.bukkit.OfflinePlayer;
import org.bukkit.command.Command;
import org.bukkit.command.CommandExecutor;
import org.bukkit.command.CommandSender;
import org.bukkit.entity.Player;

/**
 * Allows for the specified player to be trusted to a specified container.
 */
public class CommandTrust implements CommandExecutor {

	/**
	 * @param sender The user that entered the command.
	 * @param command The command itself.
	 * @param alias The alias of the command.
	 * @param args The arguments of the command.
	 * @return Confirmation of whether the command was a success or not.
	 */
	@Override
	public boolean onCommand(CommandSender sender, Command command, String alias, String[] args) {
		if (args.length == 0) {
			sender.sendMessage(ChatUtils.chatMessage("&cYou must specify a player to trust!"));
			return true;
		} else {
			if (sender instanceof Player player) {
				boolean isPlayerFound = false;
				// Does the player exist
				for (OfflinePlayer offlinePlayer : Bukkit.getOfflinePlayers()) {
					if (AranarthUtils.getPlayer(offlinePlayer.getUniqueId()) != null) {
						if (offlinePlayer.getName().equalsIgnoreCase(args[0])) {
							AranarthPlayer aranarthPlayer = AranarthUtils.getPlayer(player.getUniqueId());
							aranarthPlayer.setTrustedPlayerUUID(offlinePlayer.getUniqueId());
							aranarthPlayer.setUntrustedPlayerUUID(null);
							aranarthPlayer.setUnlockingContainer(false);
							aranarthPlayer.setLockingContainer(false);
							AranarthUtils.setPlayer(player.getUniqueId(), aranarthPlayer);
							player.sendMessage(ChatUtils.chatMessage("&7Right-click the container to trust &e" + offlinePlayer.getName()));
							isPlayerFound = true;
							return true;
						}
					}
				}
				if (!isPlayerFound) {
					sender.sendMessage(ChatUtils.chatMessage("&cThis player does not exist!"));
					return true;
				}
			}
		}
		return false;
	}

}
