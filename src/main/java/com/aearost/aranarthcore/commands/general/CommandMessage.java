package com.aearost.aranarthcore.commands.general;

import com.aearost.aranarthcore.network.NetworkManager;
import com.aearost.aranarthcore.network.NetworkPlayer;
import com.aearost.aranarthcore.objects.AranarthPlayer;
import com.aearost.aranarthcore.utils.AranarthUtils;
import com.aearost.aranarthcore.utils.ChatUtils;
import com.aearost.aranarthcore.utils.Lang;
import net.kyori.adventure.text.Component;
import net.kyori.adventure.text.serializer.legacy.LegacyComponentSerializer;
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
				player.sendMessage(ChatUtils.chatMessage(Lang.get("general.invalid_syntax", "usage", "msg <player> <message>")));
				return true;
			} else {
				UUID targetUuid = AranarthUtils.getUUIDFromUsernameOrNickname(args[0]);
				// Fall back to remote roster for players who have never joined this server
				if (targetUuid == null && NetworkManager.isActive()) {
					for (NetworkPlayer np : NetworkManager.getInstance().getRemoteRoster().values()) {
						if (np.getUsername().equalsIgnoreCase(args[0])
								|| (!np.getNickname().isEmpty() && ChatUtils.stripColorFormatting(np.getNickname()).equalsIgnoreCase(args[0]))) {
							targetUuid = np.getUuid();
							break;
						}
					}
				}
				// If the player has played before
				if (targetUuid != null) {
					Player target = Bukkit.getPlayer(targetUuid);
					AranarthPlayer aranarthPlayer = AranarthUtils.getPlayer(player.getUniqueId());
					AranarthPlayer targetAranarthPlayer = AranarthUtils.getPlayer(targetUuid);
					if (targetAranarthPlayer != null && targetAranarthPlayer.isTogglingMessages() && aranarthPlayer.getCouncilRank() == 0) {
						String targetNick = targetAranarthPlayer.getNickname().isEmpty() ? args[0] : ChatUtils.stripColorFormatting(targetAranarthPlayer.getNickname());
						player.sendMessage(ChatUtils.chatMessage(Lang.get("message.toggling", "player", targetNick)));
						return true;
					}

					// If the player is online locally
					if (target != null) {
						if (ChatUtils.isPlayerMuted(player)) {
							player.sendMessage(ChatUtils.chatMessage(Lang.get("message.muted")));
							return true;
						}
						ChatUtils.sendPrivateMessage(player, target, args, false);
						return true;
					}

					// Check if the player is on another server
					if (NetworkManager.isActive()) {
						NetworkPlayer remoteTarget = NetworkManager.getInstance().getRemotePlayer(targetUuid);
						if (remoteTarget != null) {
							if (ChatUtils.isPlayerMuted(player)) {
								player.sendMessage(ChatUtils.chatMessage(Lang.get("message.muted")));
								return true;
							}

							// Assemble message text
							StringBuilder msg = new StringBuilder();
							for (int i = 1; i < args.length; i++) {
								msg.append(args[i]);
								if (i < args.length - 1) msg.append(" ");
							}
							String assembledMsg = msg.toString();

							// Format message text
							String formattedMsg;
							if (player.hasPermission("aranarth.chat.hex")) {
								formattedMsg = ChatUtils.translateToColor(assembledMsg);
							} else if (player.hasPermission("aranarth.chat.color")) {
								formattedMsg = ChatUtils.playerColorChat(assembledMsg);
							} else {
								formattedMsg = assembledMsg;
							}

							String senderNickname = aranarthPlayer.getNickname().isEmpty() ? player.getName() : aranarthPlayer.getNickname();
							String targetNickname = remoteTarget.getNickname().isEmpty() ? remoteTarget.getUsername() : remoteTarget.getNickname();

							// Show confirmation to sender
							String prefixStart = "&7⊰&r";
							String prefixEnd = "&7⊱&r";
							Component senderPrefixComp = LegacyComponentSerializer.legacySection().deserialize(ChatUtils.translateToColor(prefixStart + "&7&l&oTo: &r&e" + targetNickname + prefixEnd + " &7&o>> &e&o"));
							Component senderMsgComp = senderPrefixComp.append(LegacyComponentSerializer.legacySection().deserialize(ChatUtils.translateToColor("&e&o") + formattedMsg));
							player.sendMessage(ChatUtils.clickableCommand(senderMsgComp, ChatUtils.translateToColor("&7Message &e" + targetNickname), "/msg " + remoteTarget.getUsername() + " ", true));

							// Store last received for /reply on sender side
							aranarthPlayer.setLastReceivedMessage(targetUuid);
							AranarthUtils.setPlayer(player.getUniqueId(), aranarthPlayer);

							// Publish to other server
							NetworkManager.getInstance().publishDirectMessage(
									player.getUniqueId(), senderNickname, targetUuid, formattedMsg);
							return true;
						}
					}

					String displayName = targetAranarthPlayer != null && !targetAranarthPlayer.getNickname().isEmpty()
							? ChatUtils.stripColorFormatting(targetAranarthPlayer.getNickname()) : args[0];
					player.sendMessage(ChatUtils.chatMessage(Lang.get("player.offline", "name", displayName)));
					return true;
				} else {
					player.sendMessage(ChatUtils.chatMessage(Lang.get("player.not_found", "name", args[0])));
					return true;
				}
			}
		} else {
			sender.sendMessage(ChatUtils.chatMessage(Lang.get("general.no_permission_console")));
			return true;
		}
	}
}
