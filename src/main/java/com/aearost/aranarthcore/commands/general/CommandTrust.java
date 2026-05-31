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
							// Toggle off if already in trust mode for this player
							if (offlinePlayer.getUniqueId().equals(aranarthPlayer.getTrustedPlayerUUID())) {
								aranarthPlayer.setTrustedPlayerUUID(null);
								AranarthUtils.setPlayer(player.getUniqueId(), aranarthPlayer);
								sender.sendMessage(ChatUtils.chatMessage("&7You are no longer in trust mode"));
							} else {
								aranarthPlayer.setTrustedPlayerUUID(offlinePlayer.getUniqueId());
								aranarthPlayer.setUntrustedPlayerUUID(null);
								aranarthPlayer.setUnlockingContainer(false);
								aranarthPlayer.setLockingContainer(false);
								AranarthUtils.setPlayer(player.getUniqueId(), aranarthPlayer);
								String nickname = AranarthUtils.getPlayer(offlinePlayer.getUniqueId()).getNickname();
								sender.sendMessage(ChatUtils.chatMessage("&7You are now trusting &e" + nickname
										+ " &7to your containers - right-click to trust them"));
								sender.sendMessage(ChatUtils.chatMessage("&7Run &e/trust &7again to exit trust mode"));
							}
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
