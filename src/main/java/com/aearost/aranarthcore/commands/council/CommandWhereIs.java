package com.aearost.aranarthcore.commands.council;

import com.aearost.aranarthcore.database.DatabaseManager;
import com.aearost.aranarthcore.network.NetworkManager;
import com.aearost.aranarthcore.network.NetworkPlayer;
import com.aearost.aranarthcore.utils.ChatUtils;
import com.aearost.aranarthcore.utils.Lang;
import org.bukkit.Bukkit;
import org.bukkit.Location;
import org.bukkit.OfflinePlayer;
import org.bukkit.command.CommandSender;
import org.bukkit.entity.Player;

import java.util.Map;
import java.util.UUID;

/**
 * Allows specified players to view the current location of another player.
 * Shows live location for players online on this server, server name for players
 * on a remote server, and last known logout location for offline players.
 */
public class CommandWhereIs {

	/**
	 * @param sender The user that entered the command.
	 * @param args The arguments of the command.
	 */
	public static boolean onCommand(CommandSender sender, String[] args) {
		if (sender instanceof Player player) {
			if (!player.hasPermission("aranarth.whereis")) {
				player.sendMessage(ChatUtils.chatMessage(Lang.get("general.no_permission")));
				return true;
			}
		}

		if (args.length == 1) {
			sender.sendMessage(ChatUtils.chatMessage(Lang.get("general.specify_player")));
			return true;
		}

		String targetName = args[1];

		// 1. Check online players on this server
		for (Player onlinePlayer : Bukkit.getOnlinePlayers()) {
			if (targetName.equalsIgnoreCase(onlinePlayer.getName())) {
				Location location = onlinePlayer.getLocation();
				sender.sendMessage(ChatUtils.chatMessage(Lang.get("whereis.online_local",
						"player", onlinePlayer.getDisplayName(),
						"world", location.getWorld().getName(),
						"x", String.valueOf(location.getBlockX()),
						"y", String.valueOf(location.getBlockY()),
						"z", String.valueOf(location.getBlockZ()))));
				return true;
			}
		}

		// 2. Check players online on remote servers
		if (NetworkManager.isActive()) {
			for (Map.Entry<UUID, NetworkPlayer> entry : NetworkManager.getInstance().getRemoteRoster().entrySet()) {
				NetworkPlayer np = entry.getValue();
				if (targetName.equalsIgnoreCase(np.getUsername())) {
					sender.sendMessage(ChatUtils.chatMessage(Lang.get("whereis.online_remote",
							"player", np.getUsername(),
							"server", np.getServer().toUpperCase())));
					return true;
				}
			}
		}

		// 3. Look up UUID for offline lookup via Bukkit's offline player list
		UUID uuid = null;
		for (OfflinePlayer op : Bukkit.getOfflinePlayers()) {
			if (targetName.equalsIgnoreCase(op.getName())) {
				uuid = op.getUniqueId();
				break;
			}
		}

		if (uuid != null && DatabaseManager.isActive()) {
			DatabaseManager.LastLocation last = DatabaseManager.getInstance().loadLastLocation(uuid);
			if (last != null) {
				sender.sendMessage(ChatUtils.chatMessage(Lang.get("whereis.offline",
						"player", targetName,
						"server", last.server.toUpperCase(),
						"world", last.world,
						"x", String.valueOf((int) last.x),
						"y", String.valueOf((int) last.y),
						"z", String.valueOf((int) last.z))));
				return true;
			}
		}

		sender.sendMessage(ChatUtils.chatMessage(Lang.get("player.not_found", "name", targetName)));
		return true;
	}

}
