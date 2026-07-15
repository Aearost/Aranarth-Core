package com.aearost.aranarthcore.commands.general;

import com.aearost.aranarthcore.network.NetworkManager;
import com.aearost.aranarthcore.network.NetworkPlayer;
import com.aearost.aranarthcore.objects.AranarthPlayer;
import com.aearost.aranarthcore.utils.AranarthUtils;
import com.aearost.aranarthcore.utils.ChatUtils;
import org.bukkit.*;
import org.bukkit.command.Command;
import org.bukkit.command.CommandExecutor;
import org.bukkit.command.CommandSender;
import org.bukkit.entity.Player;

import java.util.UUID;

/**
 * Replies to the last received message.
 */
public class CommandReply implements CommandExecutor {

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
				player.sendMessage(ChatUtils.chatMessage("&cInvalid syntax: &e/reply <message>"));
				return false;
			}

			AranarthPlayer aranarthPlayer = AranarthUtils.getPlayer(player.getUniqueId());
			UUID lastMessaged = aranarthPlayer.getLastReceivedMessage();
			if (lastMessaged == null) {
				player.sendMessage(ChatUtils.chatMessage("&cYou do not have any messages to reply to!"));
				return false;
			} else {
				OfflinePlayer offlinePlayer = Bukkit.getOfflinePlayer(lastMessaged);
				if (!offlinePlayer.isOnline()) {
					// Check if the target is on another server before giving up
					if (NetworkManager.isActive()) {
						NetworkPlayer remoteTarget = NetworkManager.getInstance().getRemotePlayer(lastMessaged);
						if (remoteTarget != null) {
							AranarthPlayer targetAp = AranarthUtils.getPlayer(lastMessaged);
							if (targetAp != null && targetAp.isTogglingMessages() && aranarthPlayer.getCouncilRank() == 0) {
								String targetNick = targetAp.getNickname().isEmpty()
										? remoteTarget.getUsername()
										: ChatUtils.stripColorFormatting(targetAp.getNickname());
								player.sendMessage(ChatUtils.chatMessage("&e" + targetNick + " &cis currently not receiving messages"));
								return true;
							}
							StringBuilder msg = new StringBuilder();
							for (int i = 0; i < args.length; i++) {
								msg.append(args[i]);
								if (i < args.length - 1) msg.append(" ");
							}
							String assembledMsg = msg.toString();
							String formattedMsg;
							if (player.hasPermission("aranarth.chat.hex")) {
								formattedMsg = ChatUtils.translateToColor(assembledMsg);
							} else if (player.hasPermission("aranarth.chat.color")) {
								formattedMsg = ChatUtils.playerColorChat(assembledMsg);
							} else {
								formattedMsg = assembledMsg;
							}
							String senderNickname = aranarthPlayer.getNickname().isEmpty()
									? player.getName()
									: ChatUtils.stripColorFormatting(aranarthPlayer.getNickname());
							String targetNickname = remoteTarget.getNickname().isEmpty()
									? remoteTarget.getUsername()
									: ChatUtils.stripColorFormatting(remoteTarget.getNickname());
							String prefixStart = "&7⊰&r";
							String prefixEnd = "&7⊱&r";
							String senderMsg = ChatUtils.translateToColor(prefixStart + "&7&l&oTo: &r&e" + targetNickname + prefixEnd + " &7&o>> &e&o") + formattedMsg;
							player.sendMessage(senderMsg);
							aranarthPlayer.setLastReceivedMessage(lastMessaged);
							AranarthUtils.setPlayer(player.getUniqueId(), aranarthPlayer);
							NetworkManager.getInstance().publishDirectMessage(player.getUniqueId(), senderNickname, lastMessaged, formattedMsg);
							return true;
						}
					}
					aranarthPlayer.setLastReceivedMessage(null);
					AranarthUtils.setPlayer(player.getUniqueId(), aranarthPlayer);
					player.sendMessage(ChatUtils.chatMessage("&cThis player is no longer online!"));
					return false;
				}

				AranarthPlayer targetAranarthPlayer = AranarthUtils.getPlayer(lastMessaged);
				if (targetAranarthPlayer.isTogglingMessages() && aranarthPlayer.getCouncilRank() == 0) {
					player.sendMessage(ChatUtils.chatMessage("&e" + targetAranarthPlayer.getNickname() + " &cis currently not receiving messages"));
					return true;
				}

				ChatUtils.sendPrivateMessage(player, offlinePlayer.getPlayer(), args, true);
				return true;
			}
		} else {
			sender.sendMessage(ChatUtils.chatMessage("&cYou must be a player to use this command!"));
			return false;
		}
	}
}
