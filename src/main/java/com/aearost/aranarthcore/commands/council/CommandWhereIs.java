package com.aearost.aranarthcore.commands.council;

import com.aearost.aranarthcore.database.DatabaseManager;
import com.aearost.aranarthcore.network.NetworkManager;
import com.aearost.aranarthcore.network.NetworkPlayer;
import com.aearost.aranarthcore.utils.ChatUtils;
import org.bukkit.Bukkit;
import org.bukkit.Location;
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
				player.sendMessage(ChatUtils.chatMessage("&cYou do not have permission to execute this command!"));
				return true;
			}
		}

		if (args.length == 1) {
			sender.sendMessage(ChatUtils.chatMessage("&cYou must enter a player's username!"));
			return true;
		}

		String targetName = args[1];

		// 1. Check online players on this server
		for (Player onlinePlayer : Bukkit.getOnlinePlayers()) {
			if (targetName.equalsIgnoreCase(onlinePlayer.getName())) {
				Location location = onlinePlayer.getLocation();
				sender.sendMessage(ChatUtils.chatMessage(onlinePlayer.getDisplayName()
						+ " &7is in &e" + location.getWorld().getName()
						+ " &7at &ex: " + location.getBlockX()
						+ " | y: " + location.getBlockY()
						+ " | z: " + location.getBlockZ()));
				return true;
			}
		}

		// 2. Check players online on remote servers
		if (NetworkManager.isActive()) {
			for (Map.Entry<UUID, NetworkPlayer> entry : NetworkManager.getInstance().getRemoteRoster().entrySet()) {
				NetworkPlayer np = entry.getValue();
				if (targetName.equalsIgnoreCase(np.getUsername())) {
					sender.sendMessage(ChatUtils.chatMessage("&e" + np.getUsername()
							+ " &7is currently online on the &e" + np.getServer().toUpperCase() + " &7server"));
					return true;
				}
			}
		}

		// 3. Look up UUID for offline lookup via Bukkit's offline player list
		UUID uuid = null;
		for (org.bukkit.OfflinePlayer op : Bukkit.getOfflinePlayers()) {
			if (targetName.equalsIgnoreCase(op.getName())) {
				uuid = op.getUniqueId();
				break;
			}
		}

		if (uuid != null && DatabaseManager.isActive()) {
			DatabaseManager.LastLocation last = DatabaseManager.getInstance().loadLastLocation(uuid);
			if (last != null) {
				sender.sendMessage(ChatUtils.chatMessage("&e" + targetName
						+ " &7was last seen on the &e" + last.server.toUpperCase()
						+ " &7server in &e" + last.world
						+ " &7at &ex: " + (int) last.x
						+ " | y: " + (int) last.y
						+ " | z: " + (int) last.z));
				return true;
			}
		}

		sender.sendMessage(ChatUtils.chatMessage("&cThat player could not be found!"));
		return true;
	}

}
