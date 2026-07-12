package com.aearost.aranarthcore.commands.general;

import com.aearost.aranarthcore.network.CrossServerTpContext;
import com.aearost.aranarthcore.network.NetworkManager;
import com.aearost.aranarthcore.objects.AranarthPlayer;
import com.aearost.aranarthcore.utils.AranarthUtils;
import com.aearost.aranarthcore.utils.ChatUtils;
import org.bukkit.Bukkit;
import org.bukkit.command.Command;
import org.bukkit.command.CommandExecutor;
import org.bukkit.command.CommandSender;
import org.bukkit.entity.Player;

/**
 * Allows the player to deny a pending teleport request.
 */
public class CommandTpDeny implements CommandExecutor {

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
			AranarthPlayer aranarthPlayer = AranarthUtils.getPlayer(player.getUniqueId());
			// Prioritize denying teleports to other players
			if (aranarthPlayer.getTeleportToUuid() != null) {
				Player target = Bukkit.getPlayer(aranarthPlayer.getTeleportToUuid());
				String targetNickname = AranarthUtils.getNickname(Bukkit.getOfflinePlayer(aranarthPlayer.getTeleportToUuid()));
				// If both players are still online
				if (target != null) {
					target.sendMessage(ChatUtils.chatMessage("&e" + aranarthPlayer.getNickname() + " &7has denied your teleport request"));
					player.sendMessage(ChatUtils.chatMessage("&7You have denied &e" + targetNickname + "&e's &7teleport request"));
				} else {
					player.sendMessage(ChatUtils.chatMessage("&e" + targetNickname + " &cis no longer online"));
				}
				// If the player logged off, the request should be cleared
				aranarthPlayer.setTeleportToUuid(null);
				aranarthPlayer.setTeleportFromUuid(null);
				AranarthUtils.setPlayer(player.getUniqueId(), aranarthPlayer);
				return true;
			} else if (aranarthPlayer.getTeleportFromUuid() != null) {
				Player target = Bukkit.getPlayer(aranarthPlayer.getTeleportFromUuid());
				String targetNickname = AranarthUtils.getNickname(Bukkit.getOfflinePlayer(aranarthPlayer.getTeleportFromUuid()));
				// If both players are still online
				if (target != null) {
					target.sendMessage(ChatUtils.chatMessage("&e" + aranarthPlayer.getNickname() + " &7has denied your teleport request"));
					player.sendMessage(ChatUtils.chatMessage("&7You have denied &e" + targetNickname + "&e's &7teleport request"));
				} else {
					player.sendMessage(ChatUtils.chatMessage("&e" + targetNickname + " &cis no longer online"));
				}
				// If the player logged off, the request should be cleared
				aranarthPlayer.setTeleportToUuid(null);
				aranarthPlayer.setTeleportFromUuid(null);
				AranarthUtils.setPlayer(player.getUniqueId(), aranarthPlayer);
				return true;
			} else if (NetworkManager.isActive()) {
				// Check for a cross-server request
				CrossServerTpContext ctx = NetworkManager.getInstance().getCrossServerTpContext(player.getUniqueId());
				if (ctx != null) {
					NetworkManager.getInstance().clearCrossServerTpContext(player.getUniqueId());
					String localNickname = aranarthPlayer.getNickname().isEmpty() ? player.getName() : aranarthPlayer.getNickname();
					aranarthPlayer.setTeleportFromUuid(null);
					aranarthPlayer.setTeleportToUuid(null);
					AranarthUtils.setPlayer(player.getUniqueId(), aranarthPlayer);

					player.sendMessage(ChatUtils.chatMessage("&7You have denied &e" + ctx.remotePlayerNickname() + "&7's teleport request"));
					NetworkManager.getInstance().publishTpDenied(
							player.getUniqueId(), localNickname,
							ctx.remotePlayerUuid());
				} else {
					player.sendMessage(ChatUtils.chatMessage("&cYou do not have any pending teleport requests!"));
				}
				return true;
			} else {
				player.sendMessage(ChatUtils.chatMessage("&cYou do not have any pending teleport requests!"));
				return true;
			}
		} else {
			sender.sendMessage(ChatUtils.chatMessage("&cOnly players can execute this command!"));
			return true;
		}
    }

}
