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

import java.util.UUID;

/**
 * Sends a request to teleport to the input player.
 */
public class CommandTeleport implements CommandExecutor {

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
			if (args.length == 0) {
				sender.sendMessage(ChatUtils.chatMessage("&cYou must enter a player to teleport to!"));
				return true;
			} else {
				UUID targetUUID = AranarthUtils.getUUIDFromUsernameOrNickname(args[0]);
				// Fall back to remote roster for players who have never joined this server
				if (targetUUID == null && NetworkManager.isActive()) {
					for (NetworkPlayer np : NetworkManager.getInstance().getRemoteRoster().values()) {
						if (np.getUsername().equalsIgnoreCase(args[0])
								|| (!np.getNickname().isEmpty() && ChatUtils.stripColorFormatting(np.getNickname()).equalsIgnoreCase(args[0]))) {
							targetUUID = np.getUuid();
							break;
						}
					}
				}
				if (targetUUID != null) {
					Player target = Bukkit.getPlayer(targetUUID);
					if (target != null) {
						// Same-server teleport request
						if (player.getUniqueId().equals(target.getUniqueId())) {
							player.sendMessage(ChatUtils.chatMessage("&cYou cannot teleport to yourself!"));
							return true;
						}

						AranarthPlayer targetPlayer = AranarthUtils.getPlayer(target.getUniqueId());
						if (targetPlayer.isTogglingTp()) {
							player.sendMessage(ChatUtils.chatMessage("&e" + targetPlayer.getNickname() + " &cis currently not accepting teleport requests"));
							return true;
						}

						AranarthPlayer senderPlayer = AranarthUtils.getPlayer(player.getUniqueId());
						// Clear any stale /tph request so CommandTpAccept takes the correct branch
						targetPlayer.setTeleportToUuid(null);
						targetPlayer.setTeleportFromUuid(player.getUniqueId());
						AranarthUtils.setPlayer(target.getUniqueId(), targetPlayer);
						player.sendMessage(ChatUtils.chatMessage("&7You have requested to teleport to &e" + targetPlayer.getNickname()));
						target.sendMessage(ChatUtils.chatMessage("&e" + senderPlayer.getNickname() + " &7has requested to teleport to you"));
						target.sendMessage(ChatUtils.chatMessage("&7Use &e/tpaccept &7or &e/tpdeny"));
						AranarthUtils.playTeleportSound(player);
						AranarthUtils.playTeleportSound(target);
					} else if (NetworkManager.isActive()) {
						// Target is on another server
						NetworkPlayer remoteTarget = NetworkManager.getInstance().getRemotePlayer(targetUUID);
						if (remoteTarget == null) {
							player.sendMessage(ChatUtils.chatMessage("&cThat player is not online!"));
							return true;
						}
						if (player.getUniqueId().equals(targetUUID)) {
							player.sendMessage(ChatUtils.chatMessage("&cYou cannot teleport to yourself!"));
							return true;
						}
						AranarthPlayer senderPlayer = AranarthUtils.getPlayer(player.getUniqueId());
						player.sendMessage(ChatUtils.chatMessage("&7You have requested to teleport to &e" + remoteTarget.getNickname()));
						NetworkManager.getInstance().publishTpRequest(
								player.getUniqueId(),
								senderPlayer.getNickname().isEmpty() ? player.getName() : senderPlayer.getNickname(),
								targetUUID,
								false);
						AranarthUtils.playTeleportSound(player);
					} else {
						player.sendMessage(ChatUtils.chatMessage("&cThat player is not online!"));
					}
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
