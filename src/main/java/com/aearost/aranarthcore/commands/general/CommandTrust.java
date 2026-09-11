package com.aearost.aranarthcore.commands.general;

import com.aearost.aranarthcore.network.NetworkManager;
import com.aearost.aranarthcore.network.NetworkPlayer;
import com.aearost.aranarthcore.objects.AranarthPlayer;
import com.aearost.aranarthcore.utils.AranarthUtils;
import com.aearost.aranarthcore.utils.ChatUtils;
import com.aearost.aranarthcore.utils.Lang;
import org.bukkit.Bukkit;
import org.bukkit.command.Command;
import org.bukkit.command.CommandExecutor;
import org.bukkit.command.CommandSender;
import org.bukkit.entity.Player;

import java.util.UUID;

import static com.aearost.aranarthcore.commands.general.CommandLock.scheduleToggleExpiry;

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
			sender.sendMessage(ChatUtils.chatMessage(Lang.get("general.invalid_syntax", "usage", "trust <player>")));
			return true;
		} else {
			if (sender instanceof Player player) {
				UUID targetUuid = AranarthUtils.getUUIDFromUsernameOrNickname(args[0]);
				if (targetUuid == null) {
					sender.sendMessage(ChatUtils.chatMessage(Lang.get("player.not_found", "name", args[0])));
					return true;
				}

				AranarthPlayer aranarthPlayer = AranarthUtils.getPlayer(player.getUniqueId());
				// Toggle off if already in trust mode for this player
				if (targetUuid.equals(aranarthPlayer.getTrustedPlayerUUID())) {
					aranarthPlayer.setTrustedPlayerUUID(null);
					AranarthUtils.setPlayer(player.getUniqueId(), aranarthPlayer);
					sender.sendMessage(ChatUtils.chatMessage(Lang.get("lock.mode_exit_lock")));
				} else {
					aranarthPlayer.setTrustedPlayerUUID(targetUuid);
					aranarthPlayer.setUntrustedPlayerUUID(null);
					aranarthPlayer.setUnlockingContainer(false);
					aranarthPlayer.setLockingContainer(false);
					aranarthPlayer.setContainerToggleExpiry(System.currentTimeMillis() + 5000);
					AranarthUtils.setPlayer(player.getUniqueId(), aranarthPlayer);
					String nickname = resolveDisplayName(targetUuid, args[0]);
					sender.sendMessage(ChatUtils.chatMessage(Lang.get("lock.trust_mode_enter", "player", nickname)));
					sender.sendMessage(ChatUtils.chatMessage(Lang.get("lock.trust_mode_hint")));
					scheduleToggleExpiry(player.getUniqueId());
				}
				return true;
			}
		}
		return false;
	}

	static String resolveDisplayName(UUID uuid, String fallback) {
		AranarthPlayer ap = AranarthUtils.getPlayer(uuid);
		if (ap != null) {
			return ap.getNickname();
		}
		if (NetworkManager.isActive()) {
			NetworkPlayer remote = NetworkManager.getInstance().getRemotePlayer(uuid);
			if (remote != null) {
				String nick = remote.getNickname();
				return (nick == null || nick.isEmpty()) ? remote.getUsername() : ChatUtils.stripColorFormatting(nick);
			}
		}
		String bukkit = Bukkit.getOfflinePlayer(uuid).getName();
		return bukkit != null ? bukkit : fallback;
	}

}
